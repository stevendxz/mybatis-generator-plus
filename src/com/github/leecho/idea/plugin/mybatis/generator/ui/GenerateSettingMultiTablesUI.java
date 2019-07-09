package com.github.leecho.idea.plugin.mybatis.generator.ui;

import com.github.leecho.idea.plugin.mybatis.generator.contants.PluginContants;
import com.github.leecho.idea.plugin.mybatis.generator.generate.MyBatisGenerateCommand;
import com.github.leecho.idea.plugin.mybatis.generator.model.Credential;
import com.github.leecho.idea.plugin.mybatis.generator.model.GlobalConfig;
import com.github.leecho.idea.plugin.mybatis.generator.model.TableConfig;
import com.github.leecho.idea.plugin.mybatis.generator.model.TableInfo;
import com.github.leecho.idea.plugin.mybatis.generator.setting.MyBatisGeneratorConfiguration;
import com.github.leecho.idea.plugin.mybatis.generator.util.DatabaseUtils;
import com.github.leecho.idea.plugin.mybatis.generator.util.JTextFieldHintListener;
import com.github.leecho.idea.plugin.mybatis.generator.util.StringUtils;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.database.model.RawConnectionConfig;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbTable;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.*;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.PanelUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * 插件主界面
 * Created by kangtian on 2018/8/1.
 */
public class GenerateSettingMultiTablesUI extends DialogWrapper {

    private AnActionEvent anActionEvent;
    private Project project;
    private MyBatisGeneratorConfiguration myBatisGeneratorConfiguration;
    private PsiElement[] psiElements;
    private TableConfig tableConfig;

    private JPanel contentPane = new JBPanel<>();

    private JTextField tableNameField = new JBTextField(20);

    private JButton columnSettingButton = new JButton("Column Setting");
    private TextFieldWithBrowseButton moduleRootField = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton entityModuleRootField = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton mapperModuleRootField = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton xmlModuleRootField = new TextFieldWithBrowseButton();
    private EditorTextFieldWithBrowseButton basePackageField;
    private EditorTextFieldWithBrowseButton domainPackageField;
    private EditorTextFieldWithBrowseButton mapperPackageField;
    private EditorTextFieldWithBrowseButton examplePackageField;
    private JTextField xmlPackageField = new JTextField();
    private JTextField mapperNameField = new JBTextField(20);
    private JTextField domainNameField = new JBTextField(20);
    private JTextField exampleNameField = new JBTextField(20);
    private JTextField primaryKeyField = new JBTextField(20);

    private JPanel examplePackagePanel = new JPanel();
    private JPanel exampleNamePanel = new JPanel();

    private JCheckBox offsetLimitBox = new JCheckBox("Pageable");
    private JCheckBox commentBox = new JCheckBox("Comment");
    private JCheckBox overrideBox = new JCheckBox("Overwrite");
    private JCheckBox needToStringHashcodeEqualsBox = new JCheckBox("toString/hashCode/equals");
    private JCheckBox useSchemaPrefixBox = new JCheckBox("Use Schema Prefix");
    private JCheckBox needForUpdateBox = new JCheckBox("Add ForUpdate");
    private JCheckBox annotationDAOBox = new JCheckBox("Repository Annotation");
    private JCheckBox useDAOExtendStyleBox = new JCheckBox("Parent Interface");
    private JCheckBox jsr310SupportBox = new JCheckBox("JSR310: Date and Time API");
    private JCheckBox annotationBox = new JCheckBox("JPA Annotation");
    private JCheckBox useActualColumnNamesBox = new JCheckBox("Actual-Column");
    private JCheckBox useTableNameAliasBox = new JCheckBox("Use-Alias");
    private JCheckBox useExampleBox = new JCheckBox("Use Example");
    private JCheckBox mysql8Box = new JCheckBox("MySQL 8");
    private JCheckBox lombokAnnotationBox = new JCheckBox("Lombok");
    private JCheckBox lombokBuilderAnnotationBox = new JCheckBox("Lombok Builder");
    private JCheckBox swaggerAnnotationBox = new JCheckBox("Swagger Model");
    private JBTabbedPane tabpanel = new JBTabbedPane();

    private JTextField authorField = new JBTextField(20);
    private JTextField versionField = new JBTextField(20);
    private JTextField descriptionField = new JBTextField(20);

    private JTextField modelPostfixField = new JTextField(20);
    private JTextField mapperPostfixField = new JTextField(20);
    private JTextField examplePostfixField = new JTextField(20);
    private JTextField servicePostfixField = new JTextField(20);
    private JTextField serviceInterfacePostfixField = new JTextField(20);
    private JPanel tablePanel = new JPanel();
    private JPanel allTablePanel = new JPanel();
    private JPanel selectedTablePanel = new JPanel();
    private JSplitPane jSplitPane = new JSplitPane();
    private JBList leftList = new JBList();
    private JBList rightList = new JBList();
    private JCheckBox useServiceBox = new JCheckBox("Use Service");
    private TextFieldWithBrowseButton serviceModuleRootField = new TextFieldWithBrowseButton();
    private EditorTextFieldWithBrowseButton servicePackageField;
    private EditorTextFieldWithBrowseButton serviceInterfacePackageField;
    private DefaultListModel listModel = new DefaultListModel();

    private List<TableInfo> allTablesList = new ArrayList<>();

    public GenerateSettingMultiTablesUI(AnActionEvent anActionEvent) {
        super(anActionEvent.getData(PlatformDataKeys.PROJECT));
        Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        this.anActionEvent = anActionEvent;
        this.project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        this.myBatisGeneratorConfiguration = MyBatisGeneratorConfiguration.getInstance(project);
        this.psiElements = anActionEvent.getData(LangDataKeys.PSI_ELEMENT_ARRAY);

        GlobalConfig globalConfig = myBatisGeneratorConfiguration.getGlobalConfig();
        Map<String, TableConfig> historyConfigList = myBatisGeneratorConfiguration.getTableConfigs();

        setTitle("MyBatis Generator Plus");
        //设置大小
        pack();
        setModal(true);
        if(psiElements.length > 0) {
            for(int i = 0; i < psiElements.length; i++) {
                TableInfo tableInfo = new TableInfo((DbTable) psiElements[i]);
                allTablesList.add(tableInfo);
                listModel.addElement(tableInfo.getTableName());
            }
        }

        if (tableConfig == null) {
            //初始化配置
            tableConfig = new TableConfig();
            tableConfig.setModuleRootPath(globalConfig.getModuleRootPath());
            tableConfig.setSourcePath(globalConfig.getSourcePath());
            tableConfig.setResourcePath(globalConfig.getResourcePath());
            tableConfig.setDomainPackage(globalConfig.getDomainPackage());
            tableConfig.setMapperPackage(globalConfig.getMapperPackage());
            tableConfig.setMapperPostfix(globalConfig.getMapperPostfix());
            tableConfig.setExamplePostfix(globalConfig.getExamplePostfix());
            tableConfig.setExamplePackage(globalConfig.getExamplePackage());
            tableConfig.setXmlPackage(globalConfig.getXmlPackage());

            tableConfig.setModelPostfix(globalConfig.getModelPostfix());
            tableConfig.setDomainModuleRootPath(globalConfig.getEntityModuleRootPath());
            tableConfig.setMapperModuleRootPath(globalConfig.getMapperModuleRootPath());
            tableConfig.setXmlModuleRootPath(globalConfig.getXmlModuleRootPath());

            tableConfig.setOffsetLimit(globalConfig.isOffsetLimit());
            tableConfig.setComment(globalConfig.isComment());
            tableConfig.setOverride(globalConfig.isOverride());
            tableConfig.setNeedToStringHashcodeEquals(globalConfig.isNeedToStringHashcodeEquals());
            tableConfig.setUseSchemaPrefix(globalConfig.isUseSchemaPrefix());
            tableConfig.setNeedForUpdate(globalConfig.isNeedForUpdate());
            tableConfig.setAnnotationDAO(globalConfig.isAnnotationDAO());
            tableConfig.setUseDAOExtendStyle(globalConfig.isUseDAOExtendStyle());
            tableConfig.setJsr310Support(globalConfig.isJsr310Support());
            tableConfig.setAnnotation(globalConfig.isAnnotation());
            tableConfig.setUseActualColumnNames(globalConfig.isUseActualColumnNames());
            tableConfig.setUseTableNameAlias(globalConfig.isUseTableNameAlias());
            tableConfig.setUseExample(globalConfig.isUseExample());
            tableConfig.setMysql8(globalConfig.isMysql8());
            tableConfig.setLombokAnnotation(globalConfig.isLombokAnnotation());
            tableConfig.setLombokBuilderAnnotation(globalConfig.isLombokBuilderAnnotation());
            tableConfig.setSwaggerAnnotation(globalConfig.isSwaggerAnnotation());
//            tableConfig.setPrimaryKey(primaryKey);

            tableConfig.setAuthor(globalConfig.getAuthor());
            tableConfig.setVersion(globalConfig.getVersion());
        }
        VerticalFlowLayout layoutManager = new VerticalFlowLayout(VerticalFlowLayout.TOP);
        layoutManager.setHgap(0);
        layoutManager.setVgap(0);
        contentPane.setLayout(layoutManager);
        this.initHeader();
        this.initTableListPanel();
        this.initGeneralPanel();
        this.initOptionsPanel();
//        tabpanel.add(new ColumnTablePanel(tableConfig, tableInfo));
        contentPane.add(tabpanel);
        tabpanel.setUI(new GenerateSettingTabUI());
        contentPane.setBorder(JBUI.Borders.empty());
        this.init();
    }

    @NotNull
    @Override
    protected DialogStyle getStyle() {
        return DialogStyle.COMPACT;
    }

    private List<String> validateSetting() {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isEmpty(moduleRootField.getText())) {
            errors.add("Module root must not be null");
        }

        if (StringUtils.isEmpty(entityModuleRootField.getText())) {
            errors.add("Domain module root must not be null");
        }

        if (StringUtils.isEmpty(mapperModuleRootField.getText())) {
            errors.add("Mapper module root must not be null");
        }

        if (StringUtils.isEmpty(xmlModuleRootField.getText())) {
            errors.add("Mapper XML module root must not be null");
        }

        if (StringUtils.isEmpty(domainNameField.getText())) {
            errors.add("Domain name must not be null");
        }

        if (StringUtils.isEmpty(mapperNameField.getText())) {
            errors.add("Mapper name must not be null");
        }

        if (StringUtils.isEmpty(domainPackageField.getText())) {
            errors.add("Domain package must not be null");
        }

        if (StringUtils.isEmpty(mapperPackageField.getText())) {
            errors.add("Mapper package must not be null");
        }

        if (StringUtils.isEmpty(xmlPackageField.getText())) {
            errors.add("Mapper xml package must not be null");
        }

        if (useExampleBox.getSelectedObjects() != null) {
            if (StringUtils.isEmpty(exampleNameField.getText())) {
                errors.add("Example name must not be null");
            }
            if (StringUtils.isEmpty(examplePackageField.getText())) {
                errors.add("Example package must not be null");
            }
        }
        return errors;
    }

    @Override
    protected void doOKAction() {

        List<String> errors = this.validateSetting();
        if (!errors.isEmpty()) {
            Messages.showMessageDialog("Invalid setting: \n" + String.join("\n", errors), "Mybatis Generator Plus", Messages.getWarningIcon());
            return;
        }

        DbDataSource dbDataSource = null;
        PsiElement current = psiElements[0];
        while (current != null) {
            if (DbDataSource.class.isAssignableFrom(current.getClass())) {
                dbDataSource = (DbDataSource) current;
                break;
            }
            current = current.getParent();
        }

        if (dbDataSource == null) {
            Messages.showMessageDialog(project, "Cannot get datasource", "Mybatis Generator Plus", Messages.getErrorIcon());
            return;
        }

        RawConnectionConfig connectionConfig = dbDataSource.getConnectionConfig();

        if (connectionConfig == null) {
            Messages.showMessageDialog(project, "Cannot get connection config", "Mybatis Generator Plus", Messages.getErrorIcon());
            return;
        }

        Map<String, Credential> credentials = myBatisGeneratorConfiguration.getCredentials();
        Credential credential;
        if (credentials == null || !credentials.containsKey(connectionConfig.getUrl())) {
            boolean result = getDatabaseCredential(connectionConfig);
            if (result) {
                credentials = myBatisGeneratorConfiguration.getCredentials();
                credential = credentials.get(connectionConfig.getUrl());
            } else {
                return;
            }
        } else {
            credential = credentials.get(connectionConfig.getUrl());
        }
        Callable<Exception> callable = new Callable<Exception>() {
            @Override
            public Exception call() {
                String url = connectionConfig.getUrl();
                CredentialAttributes credentialAttributes = new CredentialAttributes(PluginContants.PLUGIN_NAME + "-" + url, credential.getUsername(), this.getClass(), false);
                String password = PasswordSafe.getInstance().getPassword(credentialAttributes);
                try {
                    DatabaseUtils.testConnection(connectionConfig.getDriverClass(), connectionConfig.getUrl(), credential.getUsername(), password, mysql8Box.getSelectedObjects() != null);
                } catch (ClassNotFoundException | SQLException e) {
                    return e;
                }
                return null;
            }
        };
        FutureTask<Exception> future = new FutureTask<>(callable);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(future, "Connect to Database", true, project);
        Exception exception;
        try {
            exception = future.get();
        } catch (InterruptedException | ExecutionException e) {
            Messages.showMessageDialog(project, "Failed to connect to database \n " + e.getMessage(), "Mybatis Generator Plus", Messages.getErrorIcon());
            return;
        }
        if (exception != null) {
            Messages.showMessageDialog(project, "Failed to connect to database \n " + exception.getMessage(), "Mybatis Generator Plus", Messages.getErrorIcon());
            if (exception.getClass().equals(SQLException.class)) {
                SQLException sqlException = (SQLException) exception;
                if (sqlException.getErrorCode() == 1045) {
                    boolean result = getDatabaseCredential(connectionConfig);
                    if (result) {
                        this.doOKAction();
                        return;
                    }
                }
            }
            return;
        }

        if (overrideBox.getSelectedObjects() != null) {
            int confirm = Messages.showOkCancelDialog(project, "The exists file will be overwrite ,Confirm generate?", "Mybatis Generator Plus", Messages.getQuestionIcon());
            if (confirm == 2) {
                return;
            }
        } else {
            int confirm = Messages.showOkCancelDialog(project, "Confirm generate mybatis code?", "Mybatis Generator Plus", Messages.getQuestionIcon());
            if (confirm == 2) {
                return;
            }
        }

        super.doOKAction();

        this.generate(connectionConfig);

    }

    private boolean testConnection(RawConnectionConfig connectionConfig, Credential credential) {
        String url = connectionConfig.getUrl();
        CredentialAttributes credentialAttributes = new CredentialAttributes(PluginContants.PLUGIN_NAME + "-" + url, credential.getUsername(), this.getClass(), false);
        String password = PasswordSafe.getInstance().getPassword(credentialAttributes);
        try {
            DatabaseUtils.testConnection(connectionConfig.getDriverClass(), connectionConfig.getUrl(), credential.getUsername(), password, mysql8Box.getSelectedObjects() != null);
            return true;
        } catch (ClassNotFoundException e) {
            Messages.showMessageDialog(project, "Failed to connect to database \n " + e.getMessage(), "Mybatis Generator Plus", Messages.getErrorIcon());
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            Messages.showMessageDialog(project, "Failed to connect to database \n " + e.getMessage(), "Mybatis Generator Plus", Messages.getErrorIcon());
            if (e.getErrorCode() == 1045) {
                boolean result = getDatabaseCredential(connectionConfig);
                if (result) {
                    Map<String, Credential> credentials = myBatisGeneratorConfiguration.getCredentials();
                    return testConnection(connectionConfig, credentials.get(connectionConfig.getUrl()));
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    private boolean getDatabaseCredential(RawConnectionConfig connectionConfig) {
        DatabaseCredentialUI databaseCredentialUI = new DatabaseCredentialUI(anActionEvent.getProject(), connectionConfig.getUrl());
        return databaseCredentialUI.showAndGet();
    }

    private void initOptionsPanel() {
        JBPanel optionsPanel = new JBPanel(new GridLayout(8, 4, 10, 10));
        optionsPanel.add(offsetLimitBox);
        optionsPanel.add(commentBox);
        optionsPanel.add(overrideBox);
        optionsPanel.add(needToStringHashcodeEqualsBox);
        optionsPanel.add(useSchemaPrefixBox);
        optionsPanel.add(needForUpdateBox);
        optionsPanel.add(annotationDAOBox);
        optionsPanel.add(useDAOExtendStyleBox);
        optionsPanel.add(jsr310SupportBox);
        optionsPanel.add(annotationBox);
        optionsPanel.add(useActualColumnNamesBox);
        optionsPanel.add(useTableNameAliasBox);
        optionsPanel.add(useExampleBox);
        optionsPanel.add(mysql8Box);
        optionsPanel.add(lombokAnnotationBox);
        optionsPanel.add(lombokBuilderAnnotationBox);
        optionsPanel.add(swaggerAnnotationBox);

        useExampleBox.addChangeListener(e -> {
            exampleNamePanel.setVisible(useExampleBox.getSelectedObjects() != null);
            examplePackagePanel.setVisible(useExampleBox.getSelectedObjects() != null);
        });

        offsetLimitBox.setSelected(tableConfig.isOffsetLimit());
        commentBox.setSelected(tableConfig.isComment());
        overrideBox.setSelected(tableConfig.isOverride());
        needToStringHashcodeEqualsBox.setSelected(tableConfig.isNeedToStringHashcodeEquals());
        useSchemaPrefixBox.setSelected(tableConfig.isUseSchemaPrefix());
        needForUpdateBox.setSelected(tableConfig.isNeedForUpdate());
        annotationDAOBox.setSelected(tableConfig.isAnnotationDAO());
        useDAOExtendStyleBox.setSelected(tableConfig.isUseDAOExtendStyle());
        jsr310SupportBox.setSelected(tableConfig.isJsr310Support());
        annotationBox.setSelected(tableConfig.isAnnotation());
        useActualColumnNamesBox.setSelected(tableConfig.isUseActualColumnNames());
        useTableNameAliasBox.setSelected(tableConfig.isUseTableNameAlias());
        useExampleBox.setSelected(tableConfig.isUseExample());
        mysql8Box.setSelected(tableConfig.isMysql8());
        lombokAnnotationBox.setSelected(tableConfig.isLombokAnnotation());
        lombokBuilderAnnotationBox.setSelected(tableConfig.isLombokBuilderAnnotation());
        swaggerAnnotationBox.setSelected(tableConfig.isSwaggerAnnotation());
        optionsPanel.setName("Options");
        tabpanel.add(optionsPanel);
    }

    /**
     * 初始化Package组件
     */
    private void initHeader() {
        JPanel headerPanel = new JBPanel<>();
        headerPanel.setBorder(JBUI.Borders.empty(0, 5));
        VerticalFlowLayout layout = new VerticalFlowLayout(VerticalFlowLayout.TOP);
        layout.setVgap(0);
        headerPanel.setLayout(layout);
        JPanel moduleRootPanel = new JPanel();
        moduleRootPanel.setLayout(new BoxLayout(moduleRootPanel, BoxLayout.X_AXIS));
        JBLabel projectRootLabel = new JBLabel("Module Root:");
        projectRootLabel.setPreferredSize(new Dimension(150, 10));
        moduleRootField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                moduleRootField.setText(moduleRootField.getText().replaceAll("\\\\", "/"));
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getModuleRootPath())) {
            moduleRootField.setText(tableConfig.getModuleRootPath());
        } else {
            moduleRootField.setText(project.getBasePath());
        }
        moduleRootPanel.add(projectRootLabel);
        moduleRootPanel.add(moduleRootField);

        //Table
        /*JPanel tableNamePanel = new JPanel();
        tableNamePanel.setLayout(new BoxLayout(tableNamePanel, BoxLayout.X_AXIS));
        JLabel tableLabel = new JLabel("Table Name:");
        tableLabel.setLabelFor(tableNameField);
        tableLabel.setPreferredSize(new Dimension(150, 10));
        tableNamePanel.add(tableLabel);
        tableNamePanel.add(tableNameField);

        if (psiElements.length > 1) {
            tableNameField.addFocusListener(new JTextFieldHintListener(tableNameField, "eg:db_table"));
        } else {
            tableNameField.setText(tableName);
        }
        tableNameField.setEditable(false);
        tableNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String entityName = StringUtils.dbStringToCamelStyle(tableNameField.getText());
                domainNameField.setText(getDomainName(entityName));
                mapperNameField.setText(getMapperName(entityName));
                exampleNameField.setText(getExampleName(entityName));
            }
        });

        JPanel primaryPanel = new JPanel();
        primaryPanel.setLayout(new BoxLayout(primaryPanel, BoxLayout.X_AXIS));
        JLabel primaryKeyLabel = new JLabel("   Primary Key:");
        primaryKeyLabel.setLabelFor(primaryKeyField);
        primaryKeyLabel.setPreferredSize(new Dimension(150, 10));
        tableNamePanel.add(primaryKeyLabel);
        tableNamePanel.add(primaryKeyField);
        primaryKeyField.setText(primaryKey);
        primaryKeyField.setEditable(false);
        */
        headerPanel.add(moduleRootPanel);
//        headerPanel.add(tableNamePanel);
//        headerPanel.add(primaryPanel);
        contentPane.add(headerPanel);
    }

    private void initGeneralPanel() {
        // 实体类后缀
        JPanel modelPostfixPanel = new JPanel();
        modelPostfixPanel.setLayout(new BoxLayout(modelPostfixPanel, BoxLayout.X_AXIS));
        JBLabel modelPostfixLabel = new JBLabel("Model Postfix:");
        modelPostfixLabel.setPreferredSize(new Dimension(200, 20));
        modelPostfixPanel.add(modelPostfixLabel);
        modelPostfixPanel.add(modelPostfixField);

        // mapper后缀设置
        JPanel mapperPostfixPanel = new JPanel();
        mapperPostfixPanel.setLayout(new BoxLayout(mapperPostfixPanel, BoxLayout.X_AXIS));
        JBLabel mapperPostfixLabel = new JBLabel("Mapper Postfix:");
        mapperPostfixLabel.setPreferredSize(new Dimension(200, 20));
        mapperPostfixPanel.add(mapperPostfixLabel);
        mapperPostfixPanel.add(mapperPostfixField);

        // example类 后缀设置
        JPanel examplePostfixPanel = new JPanel();
        examplePostfixPanel.setLayout(new BoxLayout(examplePostfixPanel, BoxLayout.X_AXIS));
        JBLabel examplePostfixLabel = new JBLabel("Example Postfix:");
        examplePostfixLabel.setPreferredSize(new Dimension(200, 20));
        examplePostfixPanel.add(examplePostfixLabel);
        examplePostfixPanel.add(examplePostfixField);
        examplePostfixPanel.setVisible(tableConfig.isUseExample());

        // example类 后缀设置
//        JPanel servicePostfixPanel = new JPanel();
//        servicePostfixPanel.setLayout(new BoxLayout(servicePostfixPanel, BoxLayout.X_AXIS));
//        JBLabel servicePostfixLabel = new JBLabel("Service Postfix:");
//        servicePostfixLabel.setPreferredSize(new Dimension(200, 20));
//        servicePostfixPanel.add(servicePostfixLabel);
//        servicePostfixPanel.add(servicePostfixField);

//        JPanel domainNamePanel = new JPanel();
//        domainNamePanel.setLayout(new BoxLayout(domainNamePanel, BoxLayout.X_AXIS));
//        JLabel entityNameLabel = new JLabel("Model Postfix:");
//        entityNameLabel.setPreferredSize(new Dimension(150, 10));
//        domainNamePanel.add(entityNameLabel);
//        domainNamePanel.add(domainNameField);
//        if (psiElements.length > 1) {
//            domainNameField.addFocusListener(new JTextFieldHintListener(domainNameField, "eg:DbTable"));
//        } else {
//            domainNameField.setText(getDomainName(modelName));
//        }
//        domainNameField.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyReleased(KeyEvent e) {
//                mapperNameField.setText(getMapperName(modelName));
//                exampleNameField.setText(getExampleName(modelName));
//            }
//        });
//
//        //MapperName
//        JPanel mapperNamePanel = new JPanel();
//        mapperNamePanel.setLayout(new BoxLayout(mapperNamePanel, BoxLayout.X_AXIS));
//        JLabel mapperNameLabel = new JLabel("Mapper Postfix:");
//        mapperNameLabel.setPreferredSize(new Dimension(150, 10));
//        mapperNameLabel.setLabelFor(mapperNameField);
//        mapperNamePanel.add(mapperNameLabel);
//        mapperNamePanel.add(mapperNameField);
//        if (psiElements.length > 1) {
//            if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getMapperPostfix())) {
//                mapperNameField.addFocusListener(new JTextFieldHintListener(mapperNameField, "eg:DbTable" + tableConfig.getMapperPostfix()));
//            } else {
//                mapperNameField.addFocusListener(new JTextFieldHintListener(mapperNameField, "eg:DbTable" + "Mapper"));
//            }
//        } else {
//            mapperNameField.setText(getMapperName(modelName));
//        }
//
//        exampleNamePanel.setLayout(new BoxLayout(exampleNamePanel, BoxLayout.X_AXIS));
//        JLabel exampleNameLabel = new JLabel("Example Postfix:");
//        exampleNameLabel.setPreferredSize(new Dimension(150, 10));
//        exampleNameLabel.setLabelFor(exampleNameField);
//        exampleNamePanel.add(exampleNameLabel);
//        exampleNamePanel.add(exampleNameField);
//        if (psiElements.length > 1) {
//            if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getExamplePostfix())) {
//                exampleNameField.addFocusListener(new JTextFieldHintListener(exampleNameField, "eg:DbTable" + tableConfig.getExamplePostfix()));
//            } else {
//                exampleNameField.addFocusListener(new JTextFieldHintListener(exampleNameField, "eg:DbTable" + "Example"));
//            }
//        } else {
//            exampleNameField.setText(getExampleName(modelName));
//        }
//
//        exampleNamePanel.setVisible(tableConfig.isUseExample());


        JPanel basePackagePanel = new JPanel();
        basePackagePanel.setLayout(new BoxLayout(basePackagePanel, BoxLayout.X_AXIS));
        JBLabel basePackageLabel = new JBLabel("Base Package:");
        basePackageLabel.setPreferredSize(new Dimension(150, 10));
        basePackageField = new EditorTextFieldWithBrowseButton(project, false);
        basePackageField.addActionListener(e -> {
            final PackageChooserDialog chooser = new PackageChooserDialog("Select Base Package", project);
            chooser.selectPackage(basePackageField.getText());
            chooser.show();
            final PsiPackage psiPackage = chooser.getSelectedPackage();
            String packageName = psiPackage == null ? null : psiPackage.getQualifiedName();
            if (!StringUtils.isEmpty(packageName)) {
                basePackageField.setText(packageName);
                domainPackageField.setText(packageName + ".model");
                mapperPackageField.setText(packageName + "." + getMapperPostfix().toLowerCase());
                examplePackageField.setText(packageName + "." + getExamplePostfix().toLowerCase());
            }
        });
        basePackageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                domainPackageField.setText(basePackageField.getText() + ".model");
                mapperPackageField.setText(basePackageField.getText() + "." + getMapperPostfix().toLowerCase());
                examplePackageField.setText(basePackageField.getText() + "." + getExamplePostfix().toLowerCase());
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getBasePackage())) {
            basePackageField.setText(tableConfig.getBasePackage());
        } else {
            basePackageField.setText("");
        }
        basePackagePanel.add(basePackageLabel);
        basePackagePanel.add(basePackageField);

        this.domainPackageField = new EditorTextFieldWithBrowseButton(project, false);


        // 实体类目录设置
        JPanel entityModuleRootPanel = new JPanel();
        entityModuleRootPanel.setLayout(new BoxLayout(entityModuleRootPanel, BoxLayout.X_AXIS));
        JBLabel entityModuleRootLabel = new JBLabel("Model Root:");
        entityModuleRootLabel.setPreferredSize(new Dimension(150, 20));
        entityModuleRootField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                entityModuleRootField.setText(entityModuleRootField.getText().replaceAll("\\\\", "/"));
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getModuleRootPath())) {
            entityModuleRootField.setText(tableConfig.getModuleRootPath());
        } else {
            entityModuleRootField.setText(project.getBasePath());
        }
        entityModuleRootPanel.add(entityModuleRootLabel);
        entityModuleRootPanel.add(entityModuleRootField);

        JPanel entityPackagePanel = new JPanel();
        entityPackagePanel.setLayout(new BoxLayout(entityPackagePanel, BoxLayout.X_AXIS));
        JBLabel entityPackageLabel = new JBLabel("Model Package:");
        entityPackageLabel.setPreferredSize(new Dimension(150, 10));
        domainPackageField.addActionListener(e -> {
            final PackageChooserDialog chooser = new PackageChooserDialog("Select Entity Package", project);
            chooser.selectPackage(domainPackageField.getText());
            chooser.show();
            final PsiPackage psiPackage = chooser.getSelectedPackage();
            String packageName = psiPackage == null ? null : psiPackage.getQualifiedName();
            if (!StringUtils.isEmpty(packageName)) {
                domainPackageField.setText(packageName);
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getDomainPackage())) {
            domainPackageField.setText(tableConfig.getDomainPackage());
        } else {
            domainPackageField.setText("");
        }
        entityPackagePanel.add(entityPackageLabel);
        entityPackagePanel.add(domainPackageField);

        // 实体类目录设置
        JPanel mapperModuleRootPanel = new JPanel();
        mapperModuleRootPanel.setLayout(new BoxLayout(mapperModuleRootPanel, BoxLayout.X_AXIS));
        JBLabel mapperModuleRootLabel = new JBLabel("Mapper Root:");
        mapperModuleRootLabel.setPreferredSize(new Dimension(150, 20));
        mapperModuleRootField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                mapperModuleRootField.setText(mapperModuleRootField.getText().replaceAll("\\\\", "/"));
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getModuleRootPath())) {
            mapperModuleRootField.setText(tableConfig.getModuleRootPath());
        } else {
            mapperModuleRootField.setText(project.getBasePath());
        }
        mapperModuleRootPanel.add(mapperModuleRootLabel);
        mapperModuleRootPanel.add(mapperModuleRootField);

        JPanel mapperPackagePanel = new JPanel();
        mapperPackagePanel.setLayout(new BoxLayout(mapperPackagePanel, BoxLayout.X_AXIS));
        JLabel mapperPackageLabel = new JLabel("Mapper Package:");
        mapperPackageLabel.setPreferredSize(new Dimension(150, 10));
        mapperPackageField = new EditorTextFieldWithBrowseButton(project, false);
        mapperPackageField.addActionListener(event -> {
            final PackageChooserDialog packageChooserDialog = new PackageChooserDialog("Select Mapper Package", project);
            packageChooserDialog.selectPackage(mapperPackageField.getText());
            packageChooserDialog.show();

            final PsiPackage psiPackage = packageChooserDialog.getSelectedPackage();
            String packageName = psiPackage == null ? null : psiPackage.getQualifiedName();
            if (!StringUtils.isEmpty(packageName)) {
                mapperPackageField.setText(packageName);
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getMapperPackage())) {
            mapperPackageField.setText(tableConfig.getMapperPackage());
        } else {
            mapperPackageField.setText("");
        }
        mapperPackagePanel.add(mapperPackageLabel);
        mapperPackagePanel.add(mapperPackageField);

        examplePackagePanel.setLayout(new BoxLayout(examplePackagePanel, BoxLayout.X_AXIS));

        examplePackageField = new EditorTextFieldWithBrowseButton(project, false);
        examplePackageField.addActionListener(e -> {
            final PackageChooserDialog packageChooserDialog = new PackageChooserDialog("Select Example Package", project);
            packageChooserDialog.selectPackage(examplePackageField.getText());
            packageChooserDialog.show();

            final PsiPackage psiPackage = packageChooserDialog.getSelectedPackage();
            String packageName = psiPackage == null ? null : psiPackage.getQualifiedName();
            if (!StringUtils.isEmpty(packageName)) {
                examplePackageField.setText(packageName);
            }
        });

        JLabel examplePackageLabel = new JLabel("Example Package:");
        examplePackageLabel.setPreferredSize(new Dimension(150, 10));
        examplePackageField.setText(tableConfig.getExamplePackage());
        examplePackagePanel.add(examplePackageLabel);
        examplePackagePanel.add(examplePackageField);
        examplePackagePanel.setVisible(tableConfig.isUseExample());


        // 实体类目录设置
        JPanel xmlModuleRootPanel = new JPanel();
        xmlModuleRootPanel.setLayout(new BoxLayout(xmlModuleRootPanel, BoxLayout.X_AXIS));
        JBLabel xmlModuleRootLabel = new JBLabel("Xml Root:");
        xmlModuleRootLabel.setPreferredSize(new Dimension(150, 20));
        xmlModuleRootField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                xmlModuleRootField.setText(xmlModuleRootField.getText().replaceAll("\\\\", "/"));
            }
        });
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getModuleRootPath())) {
            xmlModuleRootField.setText(tableConfig.getModuleRootPath());
        } else {
            xmlModuleRootField.setText(project.getBasePath());
        }
        xmlModuleRootPanel.add(xmlModuleRootLabel);
        xmlModuleRootPanel.add(xmlModuleRootField);
        JPanel xmlPackagePanel = new JPanel();
        xmlPackagePanel.setLayout(new BoxLayout(xmlPackagePanel, BoxLayout.X_AXIS));
        JLabel xmlPackageLabel = new JLabel("Xml Package:");
        xmlPackageLabel.setPreferredSize(new Dimension(150, 10));
        xmlPackageField.setText(tableConfig.getXmlPackage());
        xmlPackagePanel.add(xmlPackageLabel);
        xmlPackagePanel.add(xmlPackageField);

        // 自定义注释
        JPanel authorPanel = new JPanel();
        authorPanel.setLayout(new BoxLayout(authorPanel, BoxLayout.X_AXIS));
        JLabel authorLabel = new JLabel("Author:");
        authorLabel.setPreferredSize(new Dimension(150, 10));
        authorLabel.setLabelFor(authorField);
        authorPanel.add(authorLabel);
        authorPanel.add(authorField);
        if (psiElements.length > 1) {
            if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getAuthor())) {
                authorField.addFocusListener(new JTextFieldHintListener(authorField, "eg:DbTable" + tableConfig.getAuthor()));
            } else {
                authorField.addFocusListener(new JTextFieldHintListener(authorField, "eg:DbTable" + System.getProperty("user.name")));
            }
        } else {
            authorField.setText(tableConfig.getAuthor());
        }

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));
        JLabel descriptionLabel = new JLabel("Description:");
        descriptionLabel.setPreferredSize(new Dimension(150, 10));
        descriptionLabel.setLabelFor(descriptionField);
        descriptionPanel.add(descriptionLabel);
        descriptionPanel.add(descriptionField);
        if (psiElements.length > 1) {
            if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getDescription())) {
                descriptionField.addFocusListener(new JTextFieldHintListener(descriptionField, "eg:DbTable" + tableConfig.getDescription()));
            } else {
                descriptionField.addFocusListener(new JTextFieldHintListener(descriptionField, "eg:DbTable" + "TODO"));
            }
        } else {
            descriptionField.setText(tableConfig.getDescription());
        }

        JPanel versionPanel = new JPanel();
        versionPanel.setLayout(new BoxLayout(versionPanel, BoxLayout.X_AXIS));
        JLabel versionLabel = new JLabel("Version:");
        versionLabel.setPreferredSize(new Dimension(150, 10));
        versionLabel.setLabelFor(versionField);
        versionPanel.add(versionLabel);
        versionPanel.add(versionField);
        if (psiElements.length > 1) {
            if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getVersion())) {
                versionField.addFocusListener(new JTextFieldHintListener(versionField, "eg:DbTable" + tableConfig.getVersion()));
            } else {
                versionField.addFocusListener(new JTextFieldHintListener(versionField, "eg:DbTable" + "1.0.0"));
            }
        } else {
            versionField.setText(tableConfig.getVersion());
        }

        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        generalPanel.add(new TitledSeparator("Comment"));

        JPanel commentPanel = new JPanel();
        commentPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        commentPanel.add(authorPanel);
        commentPanel.add(versionPanel);
        commentPanel.add(descriptionPanel);
        generalPanel.add(commentPanel);
        
        generalPanel.add(new TitledSeparator("Class Postfix"));

        JPanel domainPanel = new JPanel();
        domainPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        domainPanel.add(modelPostfixPanel);
        domainPanel.add(mapperPostfixPanel);
        domainPanel.add(examplePostfixPanel);
        generalPanel.add(domainPanel);

        generalPanel.add(new TitledSeparator("Package"));

        JPanel packagePanel = new JPanel();
        packagePanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        packagePanel.add(basePackagePanel);
        packagePanel.add(entityModuleRootPanel);
        packagePanel.add(entityPackagePanel);
        packagePanel.add(mapperModuleRootPanel);
        packagePanel.add(mapperPackagePanel);
        packagePanel.add(examplePackagePanel);
        packagePanel.add(xmlModuleRootPanel);
        packagePanel.add(xmlPackagePanel);
        generalPanel.add(packagePanel);
        generalPanel.setName("General");
        tabpanel.add(generalPanel);
    }

    public void initTableListPanel() {
        allTablePanel.setLayout(new BoxLayout(allTablePanel, BoxLayout.X_AXIS));
        TitledBorder leftTitledBorder = BorderFactory.createTitledBorder("All Tables");
        leftTitledBorder.setBorder(BorderFactory.createLineBorder(Color.gray, 2));
        allTablePanel.setBorder(leftTitledBorder);

        JPanel leftPanel = new JPanel();
        leftList = new JBList();
        leftList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        leftList.setModel(listModel);
        leftPanel.add(leftList);
        allTablePanel.add(leftPanel);

        selectedTablePanel.setLayout(new BoxLayout(selectedTablePanel, BoxLayout.X_AXIS));
        TitledBorder rightTitledBorder = BorderFactory.createTitledBorder("Selected Tables");
        rightTitledBorder.setBorder(BorderFactory.createLineBorder(Color.gray, 2));
        selectedTablePanel.setBorder(rightTitledBorder);

        JPanel rightPanel = new JPanel();
        rightList = new JBList();
        rightList.setModel(new DefaultListModel());
        rightPanel.add(rightList);
        selectedTablePanel.add(rightPanel);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setPreferredSize(new Dimension(30, 0));
        JButton addAllButton = new JButton(">>");
        addAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultListModel leftListModel = (DefaultListModel) leftList.getModel();
                DefaultListModel rightListModel = (DefaultListModel) rightList.getModel();
                for(Object obj:leftListModel.toArray()) {
                    rightListModel.addElement(obj);
                }
                rightList.setModel(rightListModel);
                ((DefaultListModel) leftList.getModel()).clear();
            }
        });
        JButton addButton = new JButton(">");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultListModel rightListModel = (DefaultListModel) rightList.getModel();
                for(Object obj:leftList.getSelectedValuesList()) {
                    rightListModel.addElement(obj);
                }

                rightList.setModel(rightListModel);

                DefaultListModel leftListModel = (DefaultListModel) leftList.getModel();
                for(Object obj:leftList.getSelectedValuesList()) {
                    leftListModel.removeElement(obj);
                }
                leftList.setModel(leftListModel);
            }
        });
        JButton clearAllButton = new JButton("<<");
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultListModel leftListModel = (DefaultListModel) leftList.getModel();
                DefaultListModel rightListModel = (DefaultListModel) rightList.getModel();
                for(Object obj:rightListModel.toArray()) {
                    leftListModel.addElement(obj);
                }
                leftList.setModel(leftListModel);
                ((DefaultListModel) rightList.getModel()).clear();
            }
        });
        JButton clearButton = new JButton("<");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultListModel rightListModel = (DefaultListModel) rightList.getModel();
                for(Object obj:rightList.getSelectedValuesList()) {
                    rightListModel.removeElement(obj);
                }
                rightList.setModel(rightListModel);

                DefaultListModel leftListModel = (DefaultListModel) leftList.getModel();
                for(Object obj:rightList.getSelectedValuesList()) {
                    leftListModel.addElement(obj);
                }
                leftList.setModel(leftListModel);
            }
        });

        buttonsPanel.add(addAllButton);
        buttonsPanel.add(addButton);
        buttonsPanel.add(clearButton);
        buttonsPanel.add(clearAllButton);

        Box vbox = Box.createHorizontalBox();
        allTablePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectedTablePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        allTablePanel.setPreferredSize(new Dimension(60, 0));
        buttonsPanel.setPreferredSize(new Dimension(30, 0));
        selectedTablePanel.setPreferredSize(new Dimension(60, 0));
        vbox.add(allTablePanel);
        vbox.add(buttonsPanel);
        vbox.add(selectedTablePanel);
        JPanel multiTablePanel = new JPanel();
        multiTablePanel.setLayout(new BoxLayout(multiTablePanel, BoxLayout.X_AXIS));
        multiTablePanel.setPreferredSize(new Dimension(150, 0));
        multiTablePanel.setName("Select Table");
        multiTablePanel.add(vbox);
        tabpanel.add(multiTablePanel);
    }

    public void generate(RawConnectionConfig connectionConfig) {
        tableConfig.setName(tableNameField.getText());
        tableConfig.setTableName(tableNameField.getText());
        tableConfig.setModuleRootPath(moduleRootField.getText());
        tableConfig.setDomainModuleRootPath(entityModuleRootField.getText());
        tableConfig.setMapperModuleRootPath(mapperModuleRootField.getText());
        tableConfig.setXmlModuleRootPath(xmlModuleRootField.getText());

        tableConfig.setBasePackage(basePackageField.getText());
        tableConfig.setDomainPackage(domainPackageField.getText());
        tableConfig.setMapperPackage(mapperPackageField.getText());
        tableConfig.setExamplePackage(examplePackageField.getText());
        tableConfig.setXmlPackage(xmlPackageField.getText());

        tableConfig.setMapperName(mapperNameField.getText());
        tableConfig.setDomainName(domainNameField.getText());
        tableConfig.setPrimaryKey(primaryKeyField.getText());
        tableConfig.setExampleName(exampleNameField.getText());

        tableConfig.setOffsetLimit(offsetLimitBox.getSelectedObjects() != null);
        tableConfig.setComment(commentBox.getSelectedObjects() != null);
        tableConfig.setOverride(overrideBox.getSelectedObjects() != null);
        tableConfig.setNeedToStringHashcodeEquals(needToStringHashcodeEqualsBox.getSelectedObjects() != null);
        tableConfig.setUseSchemaPrefix(useSchemaPrefixBox.getSelectedObjects() != null);
        tableConfig.setNeedForUpdate(needForUpdateBox.getSelectedObjects() != null);
        tableConfig.setAnnotationDAO(annotationDAOBox.getSelectedObjects() != null);
        tableConfig.setUseDAOExtendStyle(useDAOExtendStyleBox.getSelectedObjects() != null);
        tableConfig.setJsr310Support(jsr310SupportBox.getSelectedObjects() != null);
        tableConfig.setAnnotation(annotationBox.getSelectedObjects() != null);
        tableConfig.setUseActualColumnNames(useActualColumnNamesBox.getSelectedObjects() != null);
        tableConfig.setUseTableNameAlias(useTableNameAliasBox.getSelectedObjects() != null);
        tableConfig.setUseExample(useExampleBox.getSelectedObjects() != null);
        tableConfig.setMysql8(mysql8Box.getSelectedObjects() != null);
        tableConfig.setLombokAnnotation(lombokAnnotationBox.getSelectedObjects() != null);
        tableConfig.setLombokBuilderAnnotation(lombokBuilderAnnotationBox.getSelectedObjects() != null);
        tableConfig.setSwaggerAnnotation(swaggerAnnotationBox.getSelectedObjects() != null);
        tableConfig.setSourcePath(this.tableConfig.getSourcePath());
        tableConfig.setResourcePath(this.tableConfig.getResourcePath());

        tableConfig.setAuthor(authorField.getText());
        tableConfig.setVersion(versionField.getText());
        tableConfig.setDescription(descriptionField.getText());

        new MyBatisGenerateCommand(tableConfig).execute(project, connectionConfig);

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

    private String getMapperPostfix() {
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getMapperPostfix())) {
            return tableConfig.getMapperPostfix();
        } else {
            return "Mapper";
        }
    }

    private String getExamplePostfix() {
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getExamplePostfix())) {
            return tableConfig.getExamplePostfix();
        } else {
            return "Example";
        }
    }

    private String getExampleName(String entityName) {
        if (tableConfig != null && !StringUtils.isEmpty(tableConfig.getExamplePostfix())) {
            return entityName + tableConfig.getExamplePostfix();
        } else {
            return (entityName + "Example");
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
