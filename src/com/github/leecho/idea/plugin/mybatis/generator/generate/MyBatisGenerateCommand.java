package com.github.leecho.idea.plugin.mybatis.generator.generate;

import cn.kt.DbRemarksCommentGenerator;
import com.github.leecho.idea.plugin.mybatis.generator.model.TableConfig;
import com.github.leecho.idea.plugin.mybatis.generator.model.TableInfo;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.database.model.RawConnectionConfig;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.*;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.github.leecho.idea.plugin.mybatis.generator.model.Credential;
import com.github.leecho.idea.plugin.mybatis.generator.model.DbType;
import com.github.leecho.idea.plugin.mybatis.generator.setting.MyBatisGeneratorConfiguration;
import com.github.leecho.idea.plugin.mybatis.generator.util.StringUtils;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.config.*;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.exception.XMLParserException;
import org.mybatis.generator.internal.DefaultShellCallback;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生成mybatis相关代码
 * Created by kangtian on 2018/7/28.
 */
public class MyBatisGenerateCommand {

	//持久化的配置
	private MyBatisGeneratorConfiguration myBatisGeneratorConfiguration;
	//界面默认配置
	private TableConfig tableConfig;
	private String username;
	//数据库类型
	private String databaseType;
	//数据库驱动
	private String driverClass;
	//数据库连接url
	private String url;

	public MyBatisGenerateCommand(TableConfig tableConfig) {
		this.tableConfig = tableConfig;
	}

	/**
	 * 自动生成的主逻辑
	 *
	 * @param project
	 * @param connectionConfig
	 * @throws Exception
	 */
	public void execute(Project project, RawConnectionConfig connectionConfig) throws XMLParserException {
		this.myBatisGeneratorConfiguration = MyBatisGeneratorConfiguration.getInstance(project);

		saveConfig();//执行前 先保存一份当前配置

		driverClass = connectionConfig.getDriverClass();
		url = connectionConfig.getUrl();
		if (driverClass.contains("mysql")) {
			databaseType = "MySQL";
		} else if (driverClass.contains("oracle")) {
			databaseType = "Oracle";
		} else if (driverClass.contains("postgresql")) {
			databaseType = "PostgreSQL";
		} else if (driverClass.contains("sqlserver")) {
			databaseType = "SqlServer";
		} else if (driverClass.contains("sqlite")) {
			databaseType = "Sqlite";
		} else if (driverClass.contains("mariadb")) {
			databaseType = "MariaDB";
		}

		JDBCConnectionConfiguration jdbcConfig = buildJdbcConfig();
		if (jdbcConfig == null) {
			return;
		}

		if(this.tableConfig.getTableList() != null && this.tableConfig.getTableList().size() > 0) {
			for(int i = 0; i < this.tableConfig.getTableList().size(); i++ ) {
				Configuration configuration = new Configuration();
				List<String> warnings = new ArrayList<>();
				Context context = new Context(ModelType.CONDITIONAL);
				configuration.addContext(context);

				context.setId("myid");
				context.addProperty("autoDelimitKeywords", "true");
				context.addProperty("beginningDelimiter", "`");
				context.addProperty("endingDelimiter", "`");
				context.addProperty("javaFileEncoding", "UTF-8");
				context.addProperty(PropertyRegistry.CONTEXT_JAVA_FILE_ENCODING, "UTF-8");
				context.setTargetRuntime("MyBatis3");

				List<TableConfiguration> tableConfigurationList = buildTableListConfig(context);
				TableConfiguration tableConfig = tableConfigurationList.get(i);
				TableInfo info = this.tableConfig.getTableList().get(i);
				String tableName = info.getTableName();
				String realTableName;
				if (this.tableConfig.getTablePrefix() != null && tableName.startsWith(this.tableConfig.getTablePrefix())) {
					realTableName = tableName.substring(this.tableConfig.getTablePrefix().length());
				} else {
					realTableName = tableName;
				}
				String entityName = StringUtils.dbStringToCamelStyle(realTableName);
				String primaryKey = "";
				if (info.getPrimaryKeys().size() > 0) {
					primaryKey = info.getPrimaryKeys().get(0);
				}
				this.tableConfig.setPrimaryKey(primaryKey);
				this.tableConfig.setTableName(tableName);
				this.tableConfig.setDomainName(getDomainName(entityName));
				this.tableConfig.setMapperName(getMapperName(entityName));
				this.tableConfig.setExampleName(getExampleName(entityName));
				this.tableConfig.setServiceName(getServiceName(entityName));
				this.tableConfig.setServiceImplName(getServiceImplName(entityName));

				JavaModelGeneratorConfiguration modelConfig = buildModelConfig();
				SqlMapGeneratorConfiguration mapperConfig = buildMapperXmlConfig();
				JavaClientGeneratorConfiguration daoConfig = buildMapperConfig();
				CommentGeneratorConfiguration commentConfig = buildCommentConfig();
				JavaServiceGeneratorConfiguration serviceConfig = buildServiceConfig();

				context.addTableConfiguration(tableConfig);
				context.setJdbcConnectionConfiguration(jdbcConfig);
				context.setJavaModelGeneratorConfiguration(modelConfig);
				context.setSqlMapGeneratorConfiguration(mapperConfig);
				context.setJavaClientGeneratorConfiguration(daoConfig);
				context.setJavaServiceGeneratorConfiguration(serviceConfig);
				context.setCommentGeneratorConfiguration(commentConfig);
				addPluginConfiguration(context);

				createFolderForNeed(this.tableConfig);
				// override=true
				ShellCallback shellCallback;
				if (this.tableConfig.isOverride()) {
					shellCallback = new DefaultShellCallback(true);
				} else {
					shellCallback = new MergeableShellCallback(true);
				}
				Set<String> fullyQualifiedTables = new HashSet<>();
				Set<String> contexts = new HashSet<>();

				try {
					MyBatisCodeGenerator myBatisCodeGenerator = new MyBatisCodeGenerator(configuration, shellCallback, warnings);
					StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
					Balloon balloon = JBPopupFactory.getInstance()
							.createHtmlTextBalloonBuilder("Generating Code...", MessageType.INFO, null)
							.createBalloon();
					balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);

					Task.Backgroundable generateTask = new Task.Backgroundable(project, "Generating MyBatis Code", false) {
						@Override
						public void run(ProgressIndicator indicator) {
							indicator.setText("Generating MyBatis Code");
							indicator.setFraction(0.0);
							indicator.setIndeterminate(true);
							try {
								myBatisCodeGenerator.generate(new GenerateCallback(indicator, balloon), contexts, fullyQualifiedTables);
								VirtualFile baseDir = project.getBaseDir();
								baseDir.refresh(false, true);

								NotificationGroup balloonNotifications = new NotificationGroup("Mybatis Generator Plus", NotificationDisplayType.STICKY_BALLOON, true);

								List<String> result = myBatisCodeGenerator.getGeneratedJavaFiles().stream()
										.map(generatedJavaFile -> String.format("<a href=\"%s%s/%s/%s\" target=\"_blank\">%s</a>", getRelativePath(project), MyBatisGenerateCommand.this.tableConfig.getSourcePath(), generatedJavaFile.getTargetPackage().replace(".", "/"), generatedJavaFile.getFileName(), generatedJavaFile.getFileName()))
										.collect(Collectors.toList());
								result.addAll(myBatisCodeGenerator.getGeneratedXmlFiles().stream()
										.map(generatedXmlFile -> String.format("<a href=\"%s%s/%s/%s\" target=\"_blank\">%s</a>", getRelativePath(project).replace(project.getBasePath() + "/", ""), MyBatisGenerateCommand.this.tableConfig.getResourcePath(), generatedXmlFile.getTargetPackage().replace(".", "/"), generatedXmlFile.getFileName(), generatedXmlFile.getFileName()))
										.collect(Collectors.toList()));
								Notification notification = balloonNotifications.createNotification("Generate Successfully", "<html>" + String.join("<br/>", result) + "</html>", NotificationType.INFORMATION, (notification1, hyperlinkEvent) -> {
									if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
										new OpenFileDescriptor(project, Objects.requireNonNull(project.getBaseDir().findFileByRelativePath(hyperlinkEvent.getDescription()))).navigate(true);
									}
								});
								Notifications.Bus.notify(notification);
							} catch (Exception e) {
								e.printStackTrace();
								balloon.hide();
								Notification notification = new Notification("Mybatis Generator Plus", null, NotificationType.ERROR);
								notification.setTitle("Generate Failed");
								notification.setContent("Cause:" + e.getMessage());
								Notifications.Bus.notify(notification);
							}
						}
					};
					generateTask.setCancelText("Stop generate code").queue();
					generateTask.setCancelTooltipText("Stop generate mybatis code");

				} catch (Exception e) {
					e.printStackTrace();
					Messages.showMessageDialog(e.getMessage(), "Generate Failure", Messages.getInformationIcon());
				}

			}
		} else {
			List<String> warnings = new ArrayList<>();
			Configuration configuration = new Configuration();
			Context context = new Context(ModelType.CONDITIONAL);
			configuration.addContext(context);

			context.setId("myid");
			context.addProperty("autoDelimitKeywords", "true");
			context.addProperty("beginningDelimiter", "`");
			context.addProperty("endingDelimiter", "`");
			context.addProperty("javaFileEncoding", "UTF-8");
			context.addProperty(PropertyRegistry.CONTEXT_JAVA_FILE_ENCODING, "UTF-8");
			context.setTargetRuntime("MyBatis3");
			TableConfiguration tableConfig = buildTableConfig(context);
			JavaModelGeneratorConfiguration modelConfig = buildModelConfig();
			SqlMapGeneratorConfiguration mapperConfig = buildMapperXmlConfig();
			JavaClientGeneratorConfiguration daoConfig = buildMapperConfig();
			CommentGeneratorConfiguration commentConfig = buildCommentConfig();
			JavaServiceGeneratorConfiguration serviceConfig = buildServiceConfig();

			context.addTableConfiguration(tableConfig);
			context.setJdbcConnectionConfiguration(jdbcConfig);
			context.setJavaModelGeneratorConfiguration(modelConfig);
			context.setSqlMapGeneratorConfiguration(mapperConfig);
			context.setJavaClientGeneratorConfiguration(daoConfig);
			context.setJavaServiceGeneratorConfiguration(serviceConfig);
			context.setCommentGeneratorConfiguration(commentConfig);
			addPluginConfiguration(context);

			createFolderForNeed(this.tableConfig);
//			List<String> warnings = new ArrayList<>();
			// override=true
			ShellCallback shellCallback;
			if (this.tableConfig.isOverride()) {
				shellCallback = new DefaultShellCallback(true);
			} else {
				shellCallback = new MergeableShellCallback(true);
			}
			Set<String> fullyQualifiedTables = new HashSet<>();
			Set<String> contexts = new HashSet<>();

			try {
				MyBatisCodeGenerator myBatisCodeGenerator = new MyBatisCodeGenerator(configuration, shellCallback, warnings);
				StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
				Balloon balloon = JBPopupFactory.getInstance()
						.createHtmlTextBalloonBuilder("Generating Code...", MessageType.INFO, null)
						.createBalloon();
				balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);

				Task.Backgroundable generateTask = new Task.Backgroundable(project, "Generating MyBatis Code", false) {
					@Override
					public void run(ProgressIndicator indicator) {
						indicator.setText("Generating MyBatis Code");
						indicator.setFraction(0.0);
						indicator.setIndeterminate(true);
						try {
							myBatisCodeGenerator.generate(new GenerateCallback(indicator, balloon), contexts, fullyQualifiedTables);
							VirtualFile baseDir = project.getBaseDir();
							baseDir.refresh(false, true);

							NotificationGroup balloonNotifications = new NotificationGroup("Mybatis Generator Plus", NotificationDisplayType.STICKY_BALLOON, true);

							List<String> result = myBatisCodeGenerator.getGeneratedJavaFiles().stream()
									.map(generatedJavaFile -> String.format("<a href=\"%s%s/%s/%s\" target=\"_blank\">%s</a>", getRelativePath(project), MyBatisGenerateCommand.this.tableConfig.getSourcePath(), generatedJavaFile.getTargetPackage().replace(".", "/"), generatedJavaFile.getFileName(), generatedJavaFile.getFileName()))
									.collect(Collectors.toList());
							result.addAll(myBatisCodeGenerator.getGeneratedXmlFiles().stream()
									.map(generatedXmlFile -> String.format("<a href=\"%s%s/%s/%s\" target=\"_blank\">%s</a>", getRelativePath(project).replace(project.getBasePath() + "/", ""), MyBatisGenerateCommand.this.tableConfig.getResourcePath(), generatedXmlFile.getTargetPackage().replace(".", "/"), generatedXmlFile.getFileName(), generatedXmlFile.getFileName()))
									.collect(Collectors.toList()));
							Notification notification = balloonNotifications.createNotification("Generate Successfully", "<html>" + String.join("<br/>", result) + "</html>", NotificationType.INFORMATION, (notification1, hyperlinkEvent) -> {
								if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
									new OpenFileDescriptor(project, Objects.requireNonNull(project.getBaseDir().findFileByRelativePath(hyperlinkEvent.getDescription()))).navigate(true);
								}
							});
							Notifications.Bus.notify(notification);
						} catch (Exception e) {
							e.printStackTrace();
							balloon.hide();
							Notification notification = new Notification("Mybatis Generator Plus", null, NotificationType.ERROR);
							notification.setTitle("Generate Failed");
							notification.setContent("Cause:" + e.getMessage());
							Notifications.Bus.notify(notification);
						}
					}
				};
				generateTask.setCancelText("Stop generate code").queue();
				generateTask.setCancelTooltipText("Stop generate mybatis code");

			} catch (Exception e) {
				e.printStackTrace();
				Messages.showMessageDialog(e.getMessage(), "Generate Failure", Messages.getInformationIcon());
			}

		}

	}

	private String getRelativePath(Project project) {
		if (tableConfig.getModuleRootPath().equals(project.getBasePath())) {
			return "";
		} else {
			return tableConfig.getModuleRootPath().replace(project.getBasePath() + "/", "") + "/";
		}
	}


	/**
	 * 创建所需目录
	 *
	 * @param tableConfig
	 */
	private void createFolderForNeed(TableConfig tableConfig) {
		String sourcePath = tableConfig.getModuleRootPath() + "/" + tableConfig.getSourcePath() + "/";
		String resourcePath = tableConfig.getModuleRootPath() + "/" + tableConfig.getResourcePath() + "/";
		String domainPath = tableConfig.getDomainModuleRootPath() + "/" + tableConfig.getSourcePath() + "/";
		String mapperPath = tableConfig.getMapperModuleRootPath() + "/" + tableConfig.getSourcePath() + "/";
		String servicePath = tableConfig.getServiceModuleRootPath() + "/" + tableConfig.getSourcePath() + "/";
		String xmlPath = tableConfig.getXmlModuleRootPath() + "/" + tableConfig.getResourcePath() + "/";

		File sourceFile = new File(sourcePath);
		if (!sourceFile.exists() && !sourceFile.isDirectory()) {
			sourceFile.mkdirs();
		}

		File resourceFile = new File(resourcePath);
		if (!resourceFile.exists() && !resourceFile.isDirectory()) {
			resourceFile.mkdirs();
		}

		File domainSourceFile = new File(domainPath);
		if (!domainSourceFile.exists() && !domainSourceFile.isDirectory()) {
			domainSourceFile.mkdirs();
		}

		File mapperSourceFile = new File(mapperPath);
		if (!mapperSourceFile.exists() && !mapperSourceFile.isDirectory()) {
			mapperSourceFile.mkdirs();
		}

		File serviceSourceFile = new File(servicePath);
		if (!serviceSourceFile.exists() && !serviceSourceFile.isDirectory()) {
			serviceSourceFile.mkdirs();
		}

		File xmlResourceFile = new File(xmlPath);
		if (!xmlResourceFile.exists() && !xmlResourceFile.isDirectory()) {
			xmlResourceFile.mkdirs();
		}
	}


	/**
	 * 保存当前配置到历史记录
	 */
	private void saveConfig() {
		Map<String, TableConfig> historyConfigList = myBatisGeneratorConfiguration.getTableConfigs();
		if (historyConfigList == null) {
			historyConfigList = new HashMap<>();
		}
		if(tableConfig.getTableList() != null && tableConfig.getTableList().size() > 0) {
			historyConfigList.put("multiTables", tableConfig);
		} else {
			historyConfigList.put(tableConfig.getName(), tableConfig);
		}

		myBatisGeneratorConfiguration.setTableConfigs(historyConfigList);

	}

	/**
	 * 生成数据库连接配置
	 *
	 * @return
	 */
	private JDBCConnectionConfiguration buildJdbcConfig() {

		JDBCConnectionConfiguration jdbcConfig = new JDBCConnectionConfiguration();
		jdbcConfig.addProperty("nullCatalogMeansCurrent", "true");
		jdbcConfig.addProperty("remarks","true");
		jdbcConfig.addProperty("useInformationSchema","true");

		Map<String, Credential> users = myBatisGeneratorConfiguration.getCredentials();
		//if (users != null && users.containsKey(url)) {
		Credential credential = users.get(url);

		username = credential.getUsername();

		CredentialAttributes credentialAttributes = new CredentialAttributes("mybatis-generator-" + url, username, this.getClass(), false);
		String password = PasswordSafe.getInstance().getPassword(credentialAttributes);

		jdbcConfig.setUserId(username);
		jdbcConfig.setPassword(password);

		Boolean mySQL_8 = tableConfig.isMysql8();
		if (mySQL_8) {
			driverClass = DbType.MySQL_8.getDriverClass();
			url += "?serverTimezone=UTC&useSSL=false";
		} else {
			url += "?useSSL=false";
		}

		jdbcConfig.setDriverClass(driverClass);
		jdbcConfig.setConnectionURL(url);
		return jdbcConfig;
		/*} else {
			DatabaseCredentialUI databaseCredentialUI = new DatabaseCredentialUI(driverClass, url, anActionEvent, tableConfig);
			return null;
		}*/

	}

	/**
	 * 生成table配置
	 *
	 * @param context
	 * @return
	 */
	private TableConfiguration buildTableConfig(Context context) {
		TableConfiguration tableConfig = new TableConfiguration(context);
		tableConfig.setTableName(this.tableConfig.getTableName());
		tableConfig.setDomainObjectName(this.tableConfig.getDomainName());

		String schema;
		if (databaseType.equals(DbType.MySQL.name())) {
			String[] name_split = url.split("/");
			schema = name_split[name_split.length - 1];
			tableConfig.setSchema(schema);
		} else if (databaseType.equals(DbType.Oracle.name())) {
			String[] name_split = url.split(":");
			schema = name_split[name_split.length - 1];
			tableConfig.setCatalog(schema);
		} else {
			String[] name_split = url.split("/");
			schema = name_split[name_split.length - 1];
			tableConfig.setCatalog(schema);
		}

		if (!this.tableConfig.isUseExample()) {
			tableConfig.setUpdateByExampleStatementEnabled(false);
			tableConfig.setCountByExampleStatementEnabled(false);
			tableConfig.setDeleteByExampleStatementEnabled(false);
			tableConfig.setSelectByExampleStatementEnabled(false);
		}

		if (this.tableConfig.isUseSchemaPrefix()) {
			if (DbType.MySQL.name().equals(databaseType)) {
				tableConfig.setSchema(schema);
			} else if (DbType.Oracle.name().equals(databaseType)) {
				//Oracle的schema为用户名，如果连接用户拥有dba等高级权限，若不设schema，会导致把其他用户下同名的表也生成一遍导致mapper中代码重复
				tableConfig.setSchema(username);
			} else {
				tableConfig.setCatalog(schema);
			}
		}

		if(this.tableConfig.getColumnSettings().size() > 0){
			this.tableConfig.getColumnSettings().forEach((key, value) -> {
				if(value.getIgnore()){
					tableConfig.addIgnoredColumn(new IgnoredColumn(value.getColumn()));
				}	else{
					ColumnOverride override = new ColumnOverride(value.getColumn());
					override.setJavaProperty(value.getJavaProperty());
					override.setJavaType(value.getJavaType());
					override.setJdbcType(value.getJdbcType());
					tableConfig.addColumnOverride(override);
				}

			});
		}

		if ("org.postgresql.Driver".equals(driverClass)) {
			tableConfig.setDelimitIdentifiers(true);
		}

		if (!StringUtils.isEmpty(this.tableConfig.getPrimaryKey())) {
			String dbType = databaseType;
			if (DbType.MySQL.name().equals(databaseType)) {
				dbType = "JDBC";
				//dbType为JDBC，且配置中开启useGeneratedKeys时，Mybatis会使用Jdbc3KeyGenerator,
				//使用该KeyGenerator的好处就是直接在一次INSERT 语句内，通过resultSet获取得到 生成的主键值，
				//并很好的支持设置了读写分离代理的数据库
				//例如阿里云RDS + 读写分离代理 无需指定主库
				//当使用SelectKey时，Mybatis会使用SelectKeyGenerator，INSERT之后，多发送一次查询语句，获得主键值
				//在上述读写分离被代理的情况下，会得不到正确的主键
			}
			tableConfig.setGeneratedKey(new GeneratedKey(this.tableConfig.getPrimaryKey(), dbType, true, null));
		}

		if (this.tableConfig.isUseActualColumnNames()) {
			tableConfig.addProperty("useActualColumnNames", "true");
		}

		if (this.tableConfig.isUseTableNameAlias()) {
			tableConfig.setAlias(this.tableConfig.getTableName());
		}
		tableConfig.setMapperName(this.tableConfig.getMapperName());
		return tableConfig;
	}

	private List<TableConfiguration> buildTableListConfig(Context context) {
		List<TableConfiguration> configList = new ArrayList<>();
		if(tableConfig.getTableList() != null && tableConfig.getTableList().size() > 0) {
			for(TableInfo info : tableConfig.getTableList()) {
				TableConfiguration tableConfig = new TableConfiguration(context);
				String tableName = info.getTableName();
				String realTableName;
				if (this.tableConfig.getTablePrefix() != null && tableName.startsWith(this.tableConfig.getTablePrefix())) {
					realTableName = tableName.substring(this.tableConfig.getTablePrefix().length());
				} else {
					realTableName = tableName;
				}
				String entityName = StringUtils.dbStringToCamelStyle(realTableName);
				String primaryKey = "";
				if (info.getPrimaryKeys().size() > 0) {
					primaryKey = info.getPrimaryKeys().get(0);
				}
				String domainName = getDomainName(entityName);

				tableConfig.setTableName(tableName);
				tableConfig.setDomainObjectName(domainName);

				String schema;
				if (databaseType.equals(DbType.MySQL.name())) {
					String[] name_split = url.split("/");
					schema = name_split[name_split.length - 1];
					tableConfig.setSchema(schema);
				} else if (databaseType.equals(DbType.Oracle.name())) {
					String[] name_split = url.split(":");
					schema = name_split[name_split.length - 1];
					tableConfig.setCatalog(schema);
				} else {
					String[] name_split = url.split("/");
					schema = name_split[name_split.length - 1];
					tableConfig.setCatalog(schema);
				}

				if (!this.tableConfig.isUseExample()) {
					tableConfig.setUpdateByExampleStatementEnabled(false);
					tableConfig.setCountByExampleStatementEnabled(false);
					tableConfig.setDeleteByExampleStatementEnabled(false);
					tableConfig.setSelectByExampleStatementEnabled(false);
				}

				if (this.tableConfig.isUseSchemaPrefix()) {
					if (DbType.MySQL.name().equals(databaseType)) {
						tableConfig.setSchema(schema);
					} else if (DbType.Oracle.name().equals(databaseType)) {
						//Oracle的schema为用户名，如果连接用户拥有dba等高级权限，若不设schema，会导致把其他用户下同名的表也生成一遍导致mapper中代码重复
						tableConfig.setSchema(username);
					} else {
						tableConfig.setCatalog(schema);
					}
				}

				if(this.tableConfig.getColumnSettings().size() > 0){
					this.tableConfig.getColumnSettings().forEach((key, value) -> {
						if(value.getIgnore()){
							tableConfig.addIgnoredColumn(new IgnoredColumn(value.getColumn()));
						}	else{
							ColumnOverride override = new ColumnOverride(value.getColumn());
							override.setJavaProperty(value.getJavaProperty());
							override.setJavaType(value.getJavaType());
							override.setJdbcType(value.getJdbcType());
							tableConfig.addColumnOverride(override);
						}

					});
				}

				if ("org.postgresql.Driver".equals(driverClass)) {
					tableConfig.setDelimitIdentifiers(true);
				}

				if (!StringUtils.isEmpty(this.tableConfig.getPrimaryKey())) {
					String dbType = databaseType;
					if (DbType.MySQL.name().equals(databaseType)) {
						dbType = "JDBC";
						//dbType为JDBC，且配置中开启useGeneratedKeys时，Mybatis会使用Jdbc3KeyGenerator,
						//使用该KeyGenerator的好处就是直接在一次INSERT 语句内，通过resultSet获取得到 生成的主键值，
						//并很好的支持设置了读写分离代理的数据库
						//例如阿里云RDS + 读写分离代理 无需指定主库
						//当使用SelectKey时，Mybatis会使用SelectKeyGenerator，INSERT之后，多发送一次查询语句，获得主键值
						//在上述读写分离被代理的情况下，会得不到正确的主键
					}
					tableConfig.setGeneratedKey(new GeneratedKey(primaryKey, dbType, true, null));
				}

				if (this.tableConfig.isUseActualColumnNames()) {
					tableConfig.addProperty("useActualColumnNames", "true");
				}

				if (this.tableConfig.isUseTableNameAlias()) {
					tableConfig.setAlias(tableName);
				}
				tableConfig.setMapperName(this.tableConfig.getMapperName());

				configList.add(tableConfig);
			}
		}
		return configList;
	}


	/**
	 * 生成实体类配置
	 *
	 * @return
	 */
	private JavaModelGeneratorConfiguration buildModelConfig() {
		String projectFolder = tableConfig.getModuleRootPath();
		String entityPackage = tableConfig.getDomainPackage();
		String sourcePath = tableConfig.getSourcePath();
		String entityModuleRootFolder = tableConfig.getDomainModuleRootPath();

		JavaModelGeneratorConfiguration modelConfig = new JavaModelGeneratorConfiguration();

		if (!StringUtils.isEmpty(entityPackage)) {
			modelConfig.setTargetPackage(entityPackage);
		} else {
			modelConfig.setTargetPackage("");
		}
		modelConfig.setTargetProject(entityModuleRootFolder + "/" + sourcePath + "/");
		return modelConfig;
	}


	/**
	 * 生成mapper.xml文件配置
	 *
	 * @return
	 */
	private SqlMapGeneratorConfiguration buildMapperXmlConfig() {

		String projectFolder = tableConfig.getModuleRootPath();
		String mappingXMLPackage = tableConfig.getXmlPackage();
		String resourcePath = tableConfig.getResourcePath();
		String xmlModuleRootFolder = tableConfig.getXmlModuleRootPath();

		SqlMapGeneratorConfiguration mapperConfig = new SqlMapGeneratorConfiguration();

		if (!StringUtils.isEmpty(mappingXMLPackage)) {
			mapperConfig.setTargetPackage(mappingXMLPackage);
		} else {
			mapperConfig.setTargetPackage("");
		}

		mapperConfig.setTargetProject(xmlModuleRootFolder + "/" + resourcePath + "/");

		//14
		if (tableConfig.isOverride()) {
			String mappingXMLFilePath = getMappingXMLFilePath(tableConfig);
			File mappingXMLFile = new File(mappingXMLFilePath);
			if (mappingXMLFile.exists()) {
				mappingXMLFile.delete();
			}
		}

		return mapperConfig;
	}

	/**
	 * 生成mapper接口文件配置
	 *
	 * @return
	 */
	private JavaClientGeneratorConfiguration buildMapperConfig() {

		String projectFolder = tableConfig.getModuleRootPath();
		String mapperPackage = tableConfig.getMapperPackage();
		String mapperPath = tableConfig.getSourcePath();
		String mapperModuleRootFolder = tableConfig.getMapperModuleRootPath();

		JavaClientGeneratorConfiguration mapperConfig = new JavaClientGeneratorConfiguration();
		mapperConfig.setConfigurationType("XMLMAPPER");
		mapperConfig.setTargetPackage(mapperPackage);

		if (!StringUtils.isEmpty(mapperPackage)) {
			mapperConfig.setTargetPackage(mapperPackage);
		} else {
			mapperConfig.setTargetPackage("");
		}

		mapperConfig.setTargetProject(mapperModuleRootFolder + "/" + mapperPath + "/");

		return mapperConfig;
	}

	/**
	 * 生成Service接口文件配置
	 *
	 * @return
	 */
	private JavaServiceGeneratorConfiguration buildServiceConfig() {

		String projectFolder = tableConfig.getModuleRootPath();
		String servicePackage = tableConfig.getServicePackage();
		String serviceImplPackage = tableConfig.getServiceImplPackage();
		String servicePath = tableConfig.getSourcePath();
		String serviceModuleRootFolder = tableConfig.getServiceModuleRootPath();

		JavaServiceGeneratorConfiguration serviceConfig = new JavaServiceGeneratorConfiguration();
		serviceConfig.setTargetPackage(servicePackage);
		serviceConfig.setImplementationPackage(serviceImplPackage);

		if (!StringUtils.isEmpty(servicePackage)) {
			serviceConfig.setTargetPackage(servicePackage);
		} else {
			serviceConfig.setTargetPackage("");
		}
		if (!StringUtils.isEmpty(serviceImplPackage)) {
			serviceConfig.setImplementationPackage(serviceImplPackage);
		} else {
			serviceConfig.setImplementationPackage("");
		}

		serviceConfig.setTargetProject(serviceModuleRootFolder + "/" + servicePath + "/");

		return serviceConfig;
	}

	/**
	 * 生成注释配置
	 *
	 * @return
	 */
	private CommentGeneratorConfiguration buildCommentConfig() {
		CommentGeneratorConfiguration commentConfig = new CommentGeneratorConfiguration();
		commentConfig.setConfigurationType(CustomCommentGenerator.class.getName());

		if (tableConfig.isComment()) {
			commentConfig.addProperty("columnRemarks", "true");
		}
		if (tableConfig.isAnnotation()) {
			commentConfig.addProperty("annotations", "true");
		}

		commentConfig.addProperty("author", tableConfig.getAuthor());
		commentConfig.addProperty("version", tableConfig.getVersion());
		commentConfig.addProperty("description", tableConfig.getDescription());

		return commentConfig;
	}

	/**
	 * 添加相关插件（注意插件文件需要通过jar引入）
	 *
	 * @param context
	 */
	private void addPluginConfiguration(Context context) {


		//实体添加序列化
		PluginConfiguration serializablePlugin = new PluginConfiguration();
		serializablePlugin.addProperty("type", "org.mybatis.generator.plugins.SerializablePlugin");
		serializablePlugin.setConfigurationType("org.mybatis.generator.plugins.SerializablePlugin");
		context.addPluginConfiguration(serializablePlugin);


		if (tableConfig.isNeedToStringHashcodeEquals()) {
			PluginConfiguration equalsHashCodePlugin = new PluginConfiguration();
			equalsHashCodePlugin.addProperty("type", "org.mybatis.generator.plugins.EqualsHashCodePlugin");
			equalsHashCodePlugin.setConfigurationType("org.mybatis.generator.plugins.EqualsHashCodePlugin");
			context.addPluginConfiguration(equalsHashCodePlugin);
			PluginConfiguration toStringPluginPlugin = new PluginConfiguration();
			toStringPluginPlugin.addProperty("type", "org.mybatis.generator.plugins.ToStringPlugin");
			toStringPluginPlugin.setConfigurationType("org.mybatis.generator.plugins.ToStringPlugin");
			context.addPluginConfiguration(toStringPluginPlugin);
		}

		if (tableConfig.isLombokAnnotation()) {
			PluginConfiguration lombokPlugin = new PluginConfiguration();
			lombokPlugin.addProperty("type", "com.github.leecho.idea.plugin.mybatis.generator.plugin.LombokPlugin");
			lombokPlugin.setConfigurationType("com.github.leecho.idea.plugin.mybatis.generator.plugin.LombokPlugin");
			if (tableConfig.isLombokBuilderAnnotation()) {
				lombokPlugin.addProperty("builder", "true");
				lombokPlugin.addProperty("allArgsConstructor", "true");
				lombokPlugin.addProperty("noArgsConstructor", "true");
			}
			context.addPluginConfiguration(lombokPlugin);
		}
		if (tableConfig.isSwaggerAnnotation()) {
			PluginConfiguration swaggerPlugin = new PluginConfiguration();
			swaggerPlugin.addProperty("type", "com.github.leecho.idea.plugin.mybatis.generator.plugin.SwaggerPlugin");
			swaggerPlugin.setConfigurationType("com.github.leecho.idea.plugin.mybatis.generator.plugin.SwaggerPlugin");
			context.addPluginConfiguration(swaggerPlugin);
		}

		if (tableConfig.isUseExample()) {
			PluginConfiguration renameExamplePlugin = new PluginConfiguration();
			renameExamplePlugin.addProperty("type", "com.github.leecho.idea.plugin.mybatis.generator.plugin.RenameExampleClassPlugin");
			renameExamplePlugin.setConfigurationType("com.github.leecho.idea.plugin.mybatis.generator.plugin.RenameExampleClassPlugin");
			renameExamplePlugin.addProperty("target", tableConfig.getExamplePackage() + "." + this.tableConfig.getExampleName());
			context.addPluginConfiguration(renameExamplePlugin);
		}

		if (tableConfig.isUseService()) {
			PluginConfiguration renameServicePlugin = new PluginConfiguration();
			renameServicePlugin.addProperty("type", "com.github.leecho.idea.plugin.mybatis.generator.plugin.RenameServiceClassPlugin");
			renameServicePlugin.setConfigurationType("com.github.leecho.idea.plugin.mybatis.generator.plugin.RenameServiceClassPlugin");
			renameServicePlugin.addProperty("target", tableConfig.getServicePackage() + "." + this.tableConfig.getServiceName());
			renameServicePlugin.addProperty("implementation", tableConfig.getServiceImplPackage() + "." + this.tableConfig.getServiceImplName());
			context.addPluginConfiguration(renameServicePlugin);
		}


		// limit/offset插件
		if (tableConfig.isOffsetLimit()) {
			if (DbType.MySQL.name().equals(databaseType)
					|| DbType.PostgreSQL.name().equals(databaseType)) {
				PluginConfiguration mySQLLimitPlugin = new PluginConfiguration();
				mySQLLimitPlugin.addProperty("type", "cn.kt.MySQLLimitPlugin");
				mySQLLimitPlugin.setConfigurationType("cn.kt.MySQLLimitPlugin");
				context.addPluginConfiguration(mySQLLimitPlugin);
			}
		}

		//for JSR310
		if (tableConfig.isJsr310Support()) {
			JavaTypeResolverConfiguration javaTypeResolverPlugin = new JavaTypeResolverConfiguration();
			javaTypeResolverPlugin.setConfigurationType("cn.kt.JavaTypeResolverJsr310Impl");
			context.setJavaTypeResolverConfiguration(javaTypeResolverPlugin);
		}

		//forUpdate 插件
		if (tableConfig.isNeedForUpdate()) {
			if (DbType.MySQL.name().equals(databaseType)
					|| DbType.PostgreSQL.name().equals(databaseType)) {
				PluginConfiguration mySQLForUpdatePlugin = new PluginConfiguration();
				mySQLForUpdatePlugin.addProperty("type", "cn.kt.MySQLForUpdatePlugin");
				mySQLForUpdatePlugin.setConfigurationType("cn.kt.MySQLForUpdatePlugin");
				context.addPluginConfiguration(mySQLForUpdatePlugin);
			}
		}

		//repository 插件
		if (tableConfig.isAnnotationDAO()) {
			if (DbType.MySQL.name().equals(databaseType)
					|| DbType.PostgreSQL.name().equals(databaseType)) {
				PluginConfiguration repositoryPlugin = new PluginConfiguration();
				repositoryPlugin.addProperty("type", "cn.kt.RepositoryPlugin");
				repositoryPlugin.setConfigurationType("cn.kt.RepositoryPlugin");
				context.addPluginConfiguration(repositoryPlugin);
			}
		}

		//13
		if (tableConfig.isUseDAOExtendStyle()) {
			if (DbType.MySQL.name().equals(databaseType)
					|| DbType.PostgreSQL.name().equals(databaseType)) {
				PluginConfiguration commonDAOInterfacePlugin = new PluginConfiguration();
				commonDAOInterfacePlugin.addProperty("type", "cn.kt.CommonDAOInterfacePlugin");
				commonDAOInterfacePlugin.setConfigurationType("cn.kt.CommonDAOInterfacePlugin");
				context.addPluginConfiguration(commonDAOInterfacePlugin);
			}
		}

	}

	/**
	 * 获取xml文件路径 用以删除之前的xml
	 *
	 * @param tableConfig
	 * @return
	 */
	private String getMappingXMLFilePath(TableConfig tableConfig) {
		StringBuilder sb = new StringBuilder();
		String mappingXMLPackage = tableConfig.getXmlPackage();
		String xmlMvnPath = tableConfig.getResourcePath();
		sb.append(tableConfig.getXmlModuleRootPath() + "/" + xmlMvnPath + "/");

		if (!StringUtils.isEmpty(mappingXMLPackage)) {
			sb.append(mappingXMLPackage.replace(".", "/")).append("/");
		}
		if (!StringUtils.isEmpty(this.tableConfig.getMapperName())) {
			sb.append(tableConfig.getMapperName()).append(".xml");
		} else {
			sb.append(tableConfig.getDomainName()).append("Mapper.xml");
		}

		return sb.toString();
	}


	private String getDomainName(String entityName) {
		if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getModelPostfix())) {
			return entityName + tableConfig.getModelPostfix();
		} else {
			return (entityName);
		}
	}

	private String getMapperName(String entityName) {
		if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getMapperPostfix())) {
			return entityName + tableConfig.getMapperPostfix();
		} else {
			return (entityName + "Mapper");
		}
	}

	private String getExampleName(String entityName) {
		if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getExamplePostfix())) {
			return entityName + tableConfig.getExamplePostfix();
		} else {
			return (entityName + "Example");
		}
	}

	private String getServiceName(String entityName) {
		if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getServicePostfix())) {
			return entityName + tableConfig.getServicePostfix();
		} else {
			return (entityName + "Service");
		}
	}

	private String getServiceImplName(String entityName) {
		if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getServiceImplPostfix())) {
			return entityName + tableConfig.getServiceImplPostfix();
		} else {
			return (entityName + "ServiceImpl");
		}
	}
}
