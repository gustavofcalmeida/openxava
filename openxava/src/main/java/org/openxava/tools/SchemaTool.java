package org.openxava.tools;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.metamodel.*;

import org.apache.commons.io.*;
import org.apache.commons.logging.*;
import org.hibernate.boot.*;
import org.hibernate.boot.registry.*;
import org.hibernate.internal.*;
import org.hibernate.service.*;
import org.hibernate.tool.hbm2ddl.*;
import org.hibernate.tool.schema.*;
import org.openxava.jpa.*;
import org.openxava.jpa.impl.*;
import org.openxava.util.*;

/**
 * @since 5.3
 * @author Javier Paniza 
 */

public class SchemaTool {
	
	private static Log log = LogFactory.getLog(SchemaTool.class);	
	private boolean commitOnFinish = true;
	private boolean onlySequences = false; 
	private Collection<Class> annotatedClasses = null;
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0 || Is.emptyString(args[0])) {
			log.error(XavaResources.getString("schematool_action_required")); 
			return;
		}				
		if (args.length == 1 || Is.emptyString(args[1])) {
			log.error(XavaResources.getString("schematool_persistence_unit_required")); 
			return;
		}	
		XPersistence.setPersistenceUnit(args[1]);
		SchemaTool tool = new SchemaTool();
		String action = args[0];
    	if (action.equals("update")) {
    		tool.updateSchema();
    	}
    	else if (action.equals("generate")) {
    		tool.printSchema();
    	}
    	else {
   			log.error(XavaResources.getString("schematool_action_required"));     		
    	}	
		System.exit(0); // To avoid Eclipse hangs when executing Ant task
	}
	
	public void updateSchema() {
		execute(true, false);
	}
	
	public void generateSchema() {
		execute(false, false);
	}
	
	public void printSchema() {
		execute(false, true);
	}
	
	private void execute(boolean update, boolean console) { 
		try {
			Map<String, Object> factoryProperties = XPersistence.getManager().getEntityManagerFactory().getProperties();
			
			StandardServiceRegistryBuilder serviceRegistryBuilder =	new StandardServiceRegistryBuilder();
			String [] properties = {
				"hibernate.connection.driver_class", 	
				"hibernate.dialect", 	
				"hibernate.connection.url", 
				"hibernate.default_catalog", 
				"hibernate.connection.datasource",
			};
			for (String property: properties) {
				Object value = factoryProperties.get(property);
				if (value != null) {
					serviceRegistryBuilder.applySetting(property, value);
				}
			}

			String schema = (String) factoryProperties.get("hibernate.default_catalog"); 
			if (Is.emptyString(schema)) {
				schema = (String) factoryProperties.get("hibernate.default_schema"); 
				if (schema != null) {
					serviceRegistryBuilder.applySetting("hibernate.default_schema", schema); 
				}
			}			
			
			if (!Is.empty(factoryProperties.get("hibernate.connection.url"))) {
				String username = PersistenceXml.getPropetyValue(XPersistence.getPersistenceUnit(), "hibernate.connection.username");
				if (username != null) {
					serviceRegistryBuilder.applySetting("hibernate.connection.username", username);
					String password = PersistenceXml.getPropetyValue(XPersistence.getPersistenceUnit(), "hibernate.connection.password");
					if (password != null) {
						serviceRegistryBuilder.applySetting("hibernate.connection.password", password);
					}
				}
			}

			ServiceRegistry serviceRegistry = serviceRegistryBuilder.build();
			MetadataSources metadata = new MetadataSources(serviceRegistry);
			
			if (annotatedClasses != null) {
				for (Class annotatedClass: annotatedClasses) {
					metadata.addAnnotatedClass(annotatedClass);
				}						
			}
			else {
				for (ManagedType type: XPersistence.getManager().getMetamodel().getManagedTypes()) {
					Class<?> clazz = type.getJavaType();
					if (clazz == null || clazz.isInterface()) continue;
					metadata.addAnnotatedClass(clazz);
				}		
			}
			
	
			String fileName = Files.getOpenXavaBaseDir() + "ddl-" + UUID.randomUUID() + ".sql";
			File file = new File(fileName);
			java.sql.Connection connection = ((SessionImpl) XPersistence.getManager().getDelegate()).connection(); 
	    	DatabaseMetaData metaData = connection.getMetaData();
	    	boolean supportsSchemasInIndexDefinitions = supportsSchemasInIndexDefinitions(metaData);
	    	boolean supportsSemicolonAtEnd = supportsSemicolonAtEnd(metaData);
	    	XPersistence.commit();			
	    	if (update) {
				SchemaUpdate schemaUpdate = new SchemaUpdate();
				schemaUpdate.setOutputFile(fileName);
				schemaUpdate.execute(EnumSet.of(TargetType.SCRIPT), metadata.buildMetadata());
				Collection<String> scripts = FileUtils.readLines(file);
		    	for (String script: scripts) {
		    		script = addSchema(script, supportsSchemasInIndexDefinitions, schema); 
		    		script = refineScript(script, supportsSemicolonAtEnd); 
		    		log.info(XavaResources.getString("executing") + ": " + script);
		    		try {
		    			Query query = XPersistence.getManager().createNativeQuery(script); 
						query.executeUpdate();
						XPersistence.commit();
		    		}
		    		catch (Exception ex) {
		    			// In this case Hibernate logs the cause
		    			XPersistence.rollback();
		    		}
		    	}
		    	
	    	}
	    	else {
				SchemaExport schemaExport = new SchemaExport();
				schemaExport.setOutputFile(fileName);
				schemaExport.createOnly(EnumSet.of(TargetType.SCRIPT), metadata.buildMetadata());
				Collection<String> scripts = FileUtils.readLines(file);
				for (String script: scripts) {
					if (onlySequences && !script.startsWith("create sequence ")) continue;
					script = addSchema(script, supportsSchemasInIndexDefinitions, schema); 
					if (console) {
						// script = refineScript(script, supportsSemicolonAtEnd); // Not needed for console 
						System.out.println(script); 
					}
					else {
						script = refineScript(script, supportsSemicolonAtEnd); 
						log.info(XavaResources.getString("executing") + ": " + script);
						Query query = XPersistence.getManager().createNativeQuery(script);						
						query.executeUpdate();
					}
				}
	    	}
	    	FileUtils.deleteQuietly(file);
		}
		catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
		finally {
			if (commitOnFinish) XPersistence.commit();
		}

	}
		
	private String refineScript(String script, boolean supportsSemicolonAtEnd) {
		if (supportsSemicolonAtEnd) return script;
		script = script.trim();
		if (script.endsWith(";")) return script.substring(0, script.length() - 1);
		return script;
	}

	private boolean supportsSchemasInIndexDefinitions(DatabaseMetaData metaData) throws SQLException {  
		if ("PostgreSQL".equals(metaData.getDatabaseProductName())) return false;
		if ("Microsoft SQL Server".equals(metaData.getDatabaseProductName())) return false; 
		return metaData.supportsSchemasInIndexDefinitions();
	}	
	
	private boolean supportsSemicolonAtEnd(DatabaseMetaData metaData) throws SQLException { 
		return !"Oracle".equals(metaData.getDatabaseProductName());
	}	
	
	private String addSchema(String script, boolean supportsSchemasInIndexDefinitions, String schema) { 
		if (!supportsSchemasInIndexDefinitions || Is.emptyString(schema)) return script;
		// Needed at least for AS/400 where supportsSchemasInIndexDefinitions is true 
		// but the dialect does to prefix the FK on alter table, something that AS/400 requires
		return script.replace("add constraint FK", "add constraint " + schema + ".FK");
	}

	public boolean isCommitOnFinish() {
		return commitOnFinish;
	}

	public void setCommitOnFinish(boolean commitOnFinish) {
		this.commitOnFinish = commitOnFinish;
	}

	public void addAnnotatedClass(Class annotatedClass) {
		if (annotatedClasses == null) annotatedClasses = new ArrayList<Class>(); 
		annotatedClasses.add(annotatedClass);		
	}

	/** @since 6.2.2 */
	public boolean isOnlySequences() {
		return onlySequences;
	}

	/** @since 6.2.2 */
	public void setOnlySequences(boolean onlySequences) {
		this.onlySequences = onlySequences;
	}
	
}
