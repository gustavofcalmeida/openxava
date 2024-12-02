package org.openxava.web.dwr;

import java.math.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.*;

import javax.ejb.*;
import javax.servlet.http.*;
import javax.swing.table.*;

import org.apache.commons.logging.*;
import org.json.*;
import org.openxava.filters.*;
import org.openxava.formatters.*;
import org.openxava.jpa.*;
import org.openxava.model.*;
import org.openxava.model.meta.*;
import org.openxava.tab.Tab;
import org.openxava.util.*;
import org.openxava.validators.*;
import org.openxava.view.View;
import org.openxava.web.*;

/**
 * 
 * @since 7.1
 * @author Chungyen Tsai
 */

 // at field level if need, so we don't expose the inner state.
public class Calendar extends DWRBase {

	private static Log log = LogFactory.getLog(Calendar.class);

	transient private HttpServletRequest request;
	transient private HttpServletResponse response;
	private String application;
	private String module;
	private Messages errors;
	private View view;

	private Tab tab;
	private Tab tab2;
	private TableModel table;
	private DateRangeFilter filter;

	private boolean dateWithTime;
	private boolean oldLib;
	private boolean hasCondition;
	private boolean hasFilter;
	private BooleanFormatter booleanFormatter;
	private CalendarEvent event;
	private String dateName;
	private List<String> dateNameList;
	private String[] tabConditionValues;
	private String[] tabConditionComparators;

	private String[] conditionValues;
	private String[] conditionValuesTo;
	private String[] conditionComparators;

	private int keysListSize = 0;
	private int datesListSize = 0;
	private int properties1ListSize = 0;
	private int properties2ListSize = 0;
	private List<String> datesList = new ArrayList<>();
	private List<String> dateWithTimeList = Arrays.asList("java.util.Date", "java.time.LocalDateTime",
			"java.sql.Timestamp");
	private List<String> acceptedDateTypes = Arrays.asList("java.time.LocalDate", "java.util.Date", "java.sql.Date",
			"java.time.LocalDateTime", "java.sql.Timestamp");

	public String getEvents(HttpServletRequest request, HttpServletResponse response, String application, String module,
			String monthYear, String dateSimpleName) {
		try {
			initRequest(request, response, application, module);
			this.application = application;
			this.module = module;
			this.response = response;
			view = getView(request, application, module);

			String tabObject = "xava_tab";
			tab2 = getTab(request, application, module, tabObject);
			tab = tab2.clone();

			setDatesProperty(dateSimpleName);
			if (tabHasCondition(tab)) {
				hasCondition = true;
				tabGetCondition(tab);
			} else {
				hasCondition = false;
			}
			hasFilter = tab.getFilter() != null ? true : false;
			if (!hasCondition) {
				setFilter(tab, monthYear);
			}

			tab = setProperties(tab);
			this.table = tab.getAllDataTableModel();
			int tableSize = 0;
			JSONArray jsonArray = new JSONArray();
			tableSize = tab.getTableModel().getTotalSize();

			if (tableSize > 0) {
				for (int i = 0; i < tableSize; i++) {
					JSONObject jsonRow = new JSONObject();
					String d = obtainRowsDate(i);
					String[] startDate = d.split("_");
					jsonRow.put("start", startDate[1]);
					jsonRow.put("startName", startDate[0]);
					jsonRow.put("title", obtainRowsTitle(i));
					JSONObject ep = new JSONObject();
					ep.put("key", obtainRowsKey(i));
					jsonRow.put("extendedProps", ep);
					jsonArray.put(jsonRow);
				}
			} else if (tableSize == 0){
				JSONObject nullJson = new JSONObject();
				String startName = obtainRowsDate(-1).split("_")[0];
				nullJson.put("startName", startName);
				jsonArray.put(nullJson);
			} else if (tableSize == -1) {
				throw new Exception();
			}
			return jsonArray.toString();
		} catch (Exception e) {
			return "error";
		} finally {
			XPersistence.commit();
			cleanRequest();
		}
	}
	
	public String changeDateProperty(HttpServletRequest request, HttpServletResponse response, String application, String module,
			String dateSimpleName, String dateLabel, String monthYear) throws Exception {
		try {
			initRequest(request, response, application, module);
			Tab tab = getTab(request, application, module, "xava_tab");
			String prefNodeName = tab.getPreferencesNodeName("datePref.");
			Preferences preferences = Users.getCurrentPreferences();
			preferences.put(prefNodeName, dateLabel);
			preferences.put(prefNodeName + "_SimpleName", dateSimpleName);
			preferences.flush();
			String result = getEvents(request, response, application, module,
					monthYear, dateSimpleName);
			return result;
		} finally {
			XPersistence.commit();
			cleanRequest();
		}
	}

	public void dragAndDrop(HttpServletRequest request, HttpServletResponse response, String application, String module,
			String calendarKey, String dropDate, String dropDateString) throws ObjectNotFoundException, ValidationException, XavaException, SystemException, FinderException, ParseException {
		try {
			initRequest(request, response, application, module);
			View view = getView(request, application, module);
			MetaModel metaModel = view.getMetaModel();
			String[] calendarKeys = calendarKey.split("_");
			Map key = new HashMap<>();
			Map newDate = new HashMap<>();
			List<String> allKeyPropertiesNames = new ArrayList<>(metaModel.getAllKeyPropertiesNames());
			List<MetaProperty> sortedMPKeys = orderBy(metaModel.getAllMetaPropertiesKey(), allKeyPropertiesNames);
			for (int i = 0; i < calendarKeys.length; i++) {
				MetaProperty property = sortedMPKeys.get(i);
				Object keyObject = property.parse(calendarKeys[i]);
				key.put(allKeyPropertiesNames.get(i).toString(), keyObject);
			}
			MetaProperty metaProperty = metaModel.getMetaProperty(dropDateString); //for more than 1 date, have to loop
			if (isDateWithTime(metaProperty)) {
				DateTimeCombinedFormatter dtf = new DateTimeCombinedFormatter();
				newDate.put(dropDateString, dtf.parse(request, dropDate)); 
			} else {
				if (metaProperty.getTypeName().equals("java.time.LocalDate")) {
					LocalDateFormatter ldf = new LocalDateFormatter();
					dropDate = dropDate.split(" ")[0];
					newDate.put(dropDateString, ldf.parse(request, dropDate)); 
				} else {
					DateFormatter df = new DateFormatter();
					newDate.put(dropDateString, df.parse(request, dropDate)); 
				}
			}
			MapFacade.setValues(view.getModelName(), key, newDate);
		} finally {
			XPersistence.commit();
			cleanRequest();
		}
	}
	
	private DateRangeFilter setFilterForMonth(String monthYear) {
		DateRangeFilter df = new DateRangeFilter();
		String month = !monthYear.isEmpty() ? monthYear.split("_")[0] : "";
		String year = !monthYear.isEmpty() ? monthYear.split("_")[1] : "";

		Date firstDayOfMonth = getFirstDayOfMonth(month, year);
		Date lastDayOfMonth = getLastDayOfMonth(month, year);
		if (dateWithTime) {
			df.setStart(firstDayOfMonth);
			df.setEnd(lastDayOfMonth);
		} else {
			Instant f = firstDayOfMonth.toInstant();
			Instant l = lastDayOfMonth.toInstant();
			LocalDate first = f.atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate last = l.atZone(ZoneId.systemDefault()).toLocalDate();
			df.setStart(first);
			df.setEnd(last);
		}
		return df;
	}

	private static Date getFirstDayOfMonth(String month, String year) {
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		if (!month.isEmpty()) {
			calendar.set(java.util.Calendar.MONTH, Integer.parseInt(month));
			calendar.set(java.util.Calendar.YEAR, Integer.parseInt(year));
		}
		calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
		calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
		calendar.set(java.util.Calendar.MINUTE, 0);
		calendar.set(java.util.Calendar.SECOND, 0);
		calendar.set(java.util.Calendar.MILLISECOND, 0);
		calendar.add(java.util.Calendar.DAY_OF_MONTH, -6);
		return calendar.getTime();
	}

	private static Date getLastDayOfMonth(String month, String year) {
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		if (!month.isEmpty()) {
			calendar.set(java.util.Calendar.MONTH, Integer.parseInt(month));
			calendar.set(java.util.Calendar.YEAR, Integer.parseInt(year));
		}
		calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
		calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
		calendar.set(java.util.Calendar.MINUTE, 59);
		calendar.set(java.util.Calendar.SECOND, 59);
		calendar.set(java.util.Calendar.MILLISECOND, 999);
		calendar.add(java.util.Calendar.DAY_OF_MONTH, 6);
		return calendar.getTime();
	}

	private static Tab getTab(HttpServletRequest request, String application, String module, String tabOject) {
		Tab tab = (Tab) getContext(request).get(application, module, tabOject);
		request.setAttribute("xava.application", application);
		request.setAttribute("xava.module", module);
		tab.setRequest(request);
		return tab;
	}

	protected View getView(HttpServletRequest request, String application, String module) {
		View view = (View) getContext(request).get(application, module, "xava_view");
		request.setAttribute("xava.application", application);
		request.setAttribute("xava.module", module);
		view.setRequest(request);
		return view;
	}

	private String obtainRowsDate(int row) {
		//List<String> result = new ArrayList<>();
		String result ="";
		StringBuffer dateWithName = new StringBuffer();
		int i = keysListSize;
		if (datesListSize == 0)
			return result;
		if (row == -1) {
			dateWithName.append(tab.getMetaProperty(i).getQualifiedName());
			dateWithName.append("_");
			result = dateWithName.toString();
		} else {
			Object value = table.getValueAt(row, i);
			if (verifyValue(value)) {
				dateWithName.append(tab.getMetaProperty(i).getQualifiedName());
				dateWithName.append("_");
				dateWithName.append(format(value, dateWithTime, oldLib));
				result = dateWithName.toString();
			}
		}
		return result;
	}

	private String obtainRowsTitle(int row) {
		StringBuffer result = new StringBuffer();
		int i = properties1ListSize;
		int iColumn = keysListSize + datesListSize;
		int j = properties2ListSize;
		int jColumn = keysListSize + datesListSize + properties1ListSize;
		int currentColumn;
		Object value;
		for (int k = 0; k < i; k++) {
			currentColumn = iColumn + k;
			value = table.getValueAt(row, currentColumn);
			if (verifyValue(value)) {
				if (result.length() > 0)
					result.append(" / ");
				result.append(format(currentColumn, value));
			}
		}
		for (int k = 0; k < j; k++) {
			currentColumn = jColumn + k;
			value = table.getValueAt(row, currentColumn);
			if (verifyValue(value)) {
				if (result.length() > 0)
					result.append(" / ");
				result.append(format(currentColumn, value));
			}
		}
		return result.toString();
	}

	private String obtainRowsKey(int row) {
		StringBuffer keys = new StringBuffer();
		int j = keysListSize;
		Object value;
		for (int i = 0; i < j; i++) {
			value = table.getValueAt(row, i);
			if (verifyValue(value)) {
				if (keys.length() > 0) {
					keys.append("_");
				}
				keys.append(value.toString());
			}
		}
		return keys.toString();
	}

	private boolean verifyValue(Object value) {
		boolean b = true;
		if (!(value instanceof BigDecimal) && Is.empty(value))
			b = false;
		if (value instanceof byte[])
			b = false;
		return b;
	}

	private String format(int column, Object value) {
		MetaProperty p = tab.getMetaProperty(column);
		if (p.hasValidValues()) {
			return p.getValidValueLabel(value);
		} else if (p.getType().equals(boolean.class) || p.getType().equals(Boolean.class)) {
			return getBooleanFormatter().format(null, value);
		} else {
			return WebEditors.format(request, p, value, errors, view.getViewName(), true);
		}
	}

	private BooleanFormatter getBooleanFormatter() {
		if (booleanFormatter == null) {
			booleanFormatter = new BooleanFormatter();
		}
		return booleanFormatter;
	}
	
	private void setDatesProperty(String dateSimpleName) {
		setDatesPropertyFromSimpleName(dateSimpleName);
	}

	private Tab setProperties(Tab tab) {
		List<String> newTabColumn = new ArrayList<>();
		List<String> expectedNames = Arrays.asList("anyo", "year", "number", "numero", "name", "nombre", "title",
				"titulo", "description", "descripcion");
		List<String> keysList = new ArrayList<>(tab.getMetaTab().getMetaModel().getAllKeyPropertiesNames());
		List<String> properties1List = new ArrayList<>();
		List<String> properties2List = new ArrayList<>();
		int properties1ListCount = 0;
		int properties2ListCount = 0;

		newTabColumn.addAll(keysList);
		List<MetaProperty> mp = tab.getMetaPropertiesNotCalculated();
		int maxLimit = mp.size() > 2 ? 2 : 1;
		for (MetaProperty metaProperty : mp) {
			if (properties1ListCount < maxLimit && expectedNames.contains(metaProperty.getQualifiedName())) {
				properties1List.add(metaProperty.getQualifiedName());
				properties1ListCount++;
				properties2ListCount++;
			}
			if (properties2ListCount < maxLimit && !datesList.contains(metaProperty.getQualifiedName())
					&& !keysList.contains(metaProperty.getQualifiedName())
					&& !properties1List.contains(metaProperty.getQualifiedName())) {
				properties2List.add(metaProperty.getQualifiedName());
				properties2ListCount++;
			}
		}
		newTabColumn.addAll(datesList);
		newTabColumn.addAll(properties1List);

		if (properties1ListCount < maxLimit) {
			int j = 0;
			for (int i = properties1ListCount; i < maxLimit; i++) {
				if (j + 1 > properties2ListCount)
					break;
				newTabColumn.add(properties2List.get(j));
				j++;
			}
			properties2ListSize = j;
		} else {
			properties2ListSize = 0;
		}

		keysListSize = keysList.size();
		datesListSize = datesList.size();
		properties1ListSize = properties1List.size();

		if (hasCondition) {
			String[] conditionValues = tab.getConditionValues();
			String[] conditionValuesTo = tab.getConditionValuesTo();
			String[] conditionComparators = tab.getConditionComparators();
			tab.addProperties(newTabColumn, conditionValues, conditionValuesTo, conditionComparators);
		} else {
			tab.clearProperties();
			tab.addProperties(newTabColumn);
		}
		
		if (tab.getMetaTab().hasBaseCondition()) tab.addProperties(getBaseConditionPropertiesAsString(tab.getMetaTab().getBaseCondition()));
		return tab;
	}

	private boolean tabHasCondition(Tab tab) {
		boolean b = false;
		if (tab.getConditionValues() != null) {
			for (String condition : tab.getConditionValues()) {
				if (condition != null && !condition.equals("")) {
					b = true;
					break;
				}
			}
		}
		return b;
	}

	private void tabGetCondition(Tab tab) {
		conditionValues = tab.getConditionValues();
		conditionValuesTo = tab.getConditionValuesTo();
		conditionComparators = tab.getConditionComparators();
	}

	private void setFilter(Tab tab, String monthYear) {
		if (hasFilter) {
			IFilter oldFilter = tab.getFilter();
			DateRangeFilter filter = new DateRangeFilter();
			filter = setFilterForMonth(monthYear);
			tab.setFilter(new CompositeFilter(oldFilter, filter));
			tab.setBaseCondition("${" + dateName + "} between ? and ?");
		} else {
			DateRangeFilter filter = new DateRangeFilter();
			filter = setFilterForMonth(monthYear);
			tab.setFilter(filter);
			tab.setBaseCondition("${" + dateName + "} between ? and ?");
		}
	}

	public String format(Object date, boolean withTime, boolean oldLib) { // It should be private instead of public
		String format;
		if (date == null)
			return "";
		if (date instanceof String || date instanceof Number)
			return date.toString();
		if (oldLib) {
			format = withTime ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		} else {
			DateTimeFormatter formatter = withTime ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
					: DateTimeFormatter.ofPattern("yyyy-MM-dd");
			return withTime ? ((LocalDateTime) date).format(formatter) : ((LocalDate) date).format(formatter);
		}
	}
	
	private List<MetaProperty> orderBy (List<MetaProperty> metaProperties , List<String> keyPropertiesNames) {
		List<MetaProperty> l = new ArrayList<>();
		for (int i = 0; i < keyPropertiesNames.size(); i++) {
			for (int j = 0; j < metaProperties.size(); j++) {
				if (metaProperties.get(j).getName().equals(keyPropertiesNames.get(i))) {
					l.add(metaProperties.get(j));
					break;
				}
			}
		}
		return l;
	}
	
	private boolean isDateWithTime(MetaProperty property) {
		dateWithTime = dateWithTimeList.contains(property.getTypeName()) ? true : false;
		return dateWithTime;
	}
	
	private void setDatesPropertyFromSimpleName(String dateSimpleName) {
	    MetaProperty metaProperty = tab.getMetaTab().getMetaModel().getMetaProperty(dateSimpleName);
	    datesList.add(metaProperty.getName());
	    dateName = metaProperty.getName();
	    processDateProperty(metaProperty);
	}
	
	private void processDateProperty(MetaProperty metaProperty) {
	    dateWithTime = isDateWithTime(metaProperty);
	    String className = metaProperty.getTypeName();
	    if (className.startsWith("java.util.") || className.startsWith("java.sql.")) {
	        oldLib = true;
	    } else if (className.startsWith("java.time.")) {
	        oldLib = false;
	    }
	}
	
	private List<String> getBaseConditionPropertiesAsString(String baseCondition){
		Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
	    Matcher matcher = pattern.matcher(baseCondition);
	    List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
	}
	
}
