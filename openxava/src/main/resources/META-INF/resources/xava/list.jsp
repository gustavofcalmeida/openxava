<%@ include file="imports.jsp"%>

<%@ page import="org.openxava.web.WebEditors" %>
<%@ page import="org.openxava.util.XavaResources"%> 
<%@ page import="org.openxava.model.meta.MetaProperty" %>
<%@ page import="org.openxava.tab.Tab"%>
<%@ page import="org.openxava.util.Is" %>

<jsp:useBean id="context" class="org.openxava.controller.ModuleContext" scope="session"/>
<jsp:useBean id="style" class="org.openxava.web.style.Style" scope="request"/>

<%
String tabObject = request.getParameter("tabObject");
tabObject = (tabObject == null || tabObject.equals(""))?"xava_tab":tabObject;
org.openxava.tab.Tab tab = (org.openxava.tab.Tab) context.get(request, tabObject);
String editor = tab.getEditor();
String collection = request.getParameter("collection");
org.openxava.controller.ModuleManager manager = (org.openxava.controller.ModuleManager) context.get(request, "manager", "org.openxava.controller.ModuleManager");
String groupBy = tab.getGroupBy();
boolean grouping = !Is.emptyString(groupBy);
%>

<%
if (collection == null || collection.equals("")) { 	
%>
<table width="100%" class=<%=style.getListTitleWrapper()%>>
<tr><td class=<%=style.getListTitle()%>>
<% if (style.isShowModuleDescription()) { %>
<%=manager.getModuleDescription()%>
<% } %>

<jsp:include page="listConfigurations.jsp"/>

<%
if (!tab.isAllConfiguration()) { 
	if (tab.isSaveConfigurationAllowed()) { %>
<span id="xava_save_list_configuration">
<xava:link action="List.saveConfiguration"/>
</span>
<% 
   	}	 
   	else {
%>
<xava:link action="List.changeConfiguration"/>
<%
   	}
}
%>

<% 
if (tab.isTitleVisible()) { 
%> 
<% if (style.isShowModuleDescription()) { %> - <% } %>
<span id="list-title"><%=tab.getTitle()%></span> 
<%
}
%>
<% if (style.isShowRowCountOnTop() && !grouping) { // && grouping 
	int totalSize = tab.getTotalSize();
	int finalIndex = Math.min(totalSize, tab.getFinalIndex());
%>
<span class="<%=style.getHeaderListCount()%>">
<%=XavaResources.getString(request, "header_list_count", new Integer(tab.getInitialIndex() + 1), new Integer(finalIndex), new Integer(totalSize))%>
</span>
<% } %>
<% if (manager.getDialogLevel() == 0) { %>
<select class="xava_group_by">
	<option value=""><%=grouping?XavaResources.getString("no_grouping"):XavaResources.getString("no_grouping")%></option>
	<% 
	for (MetaProperty property: tab.getMetaPropertiesGroupBy()) {
		String selected = "";
		if (groupBy.equals(property.getQualifiedName())) {
			selected = "selected";
		}
	%>
	<option value="<%=property.getQualifiedName()%>" <%=selected%>><xava:message key="group_by"/> <%=property.getQualifiedLabel(request).toLowerCase()%></option>
	<%
		if (property.getType().isAssignableFrom(java.util.Date.class) || property.getType().isAssignableFrom(java.time.LocalDate.class)) { 	
			if (groupBy.equals(property.getQualifiedName() + "[month]")) {
				selected = "selected";
			}
			else {
				selected = "";
			}
	%>
	<option value="<%=property.getQualifiedName()%>[month]" <%=selected%>><xava:message key="group_by_month_of"/> <%=property.getQualifiedLabel(request).toLowerCase()%></option>
	<%
			if (groupBy.equals(property.getQualifiedName() + "[year]")) {
				selected = "selected";
			}
			else {
				selected = "";
			}
	%>		
	<option value="<%=property.getQualifiedName()%>[year]" <%=selected%>><xava:message key="group_by_year_of"/> <%=property.getQualifiedLabel(request).toLowerCase()%></option> 
	<%		
		}
	} 
	%>
</select> 
<% } %>
</td></tr>
</table>
<%
} 
%>

<jsp:include page='<%=WebEditors.getUrl(tab.getEditor(), tab.getMetaTab())%>'>
	<jsp:param name="viewKeyEditable" value='<%=request.getParameter("viewKeyEditable")%>'/>
</jsp:include>
