package com.github.leecho.idea.plugin.mybatis.generator.plugin;

import com.github.leecho.idea.plugin.mybatis.generator.util.StringUtils;
import org.mybatis.generator.api.PluginAdapter;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaTypeResolver;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.config.JavaServiceGeneratorConfiguration;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mybatis.generator.config.*;

/**
 * Create with IntelliJ IDEA
 *
 * @ProjectName: mybatis-generator-plus
 * @ClassName: RenameServiceClassPlugin
 * @Description: TODO
 * @Date: 2019/7/12 13:35
 * @Author: Adtis-D
 * @Version: 1.0.0
 */
public class RenameServiceClassPlugin extends PluginAdapter {

    /**
     * This plugin is always valid - no properties are required
     */
    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    /**
     * 生成额外java文件
     */
    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        Context context = (Context) introspectedTable.getContext();
        JavaServiceGeneratorConfiguration serviceGeneratorConfiguration;
        if ((serviceGeneratorConfiguration = context.getJavaServiceGeneratorConfiguration()) == null) {
            return null;
        }

        String targetPackage = serviceGeneratorConfiguration.getTargetPackage();
        String targetProject = serviceGeneratorConfiguration.getTargetProject();
        String implementationPackage = serviceGeneratorConfiguration.getImplementationPackage();

        CompilationUnit addServiceInterface = addServiceInterface(introspectedTable, targetPackage);
        CompilationUnit addServiceImplClazz = addServiceImplClazz(introspectedTable, targetPackage,
                implementationPackage);

        GeneratedJavaFile gjfServiceInterface = new GeneratedJavaFile(addServiceInterface, targetProject,
                this.context.getProperty("javaFileEncoding"), this.context.getJavaFormatter());
        GeneratedJavaFile gjfServiceImplClazz = new GeneratedJavaFile(addServiceImplClazz, targetProject,
                this.context.getProperty("javaFileEncoding"), this.context.getJavaFormatter());

        List<GeneratedJavaFile> list = new ArrayList<>();
        list.add(gjfServiceInterface);
        list.add(gjfServiceImplClazz);
        return list;
    }

    protected CompilationUnit addServiceInterface(IntrospectedTable introspectedTable, String targetPackage) {
        String entityClazzType = introspectedTable.getBaseRecordType();
        String serviceSuperPackage = targetPackage;
        String entityExampleClazzType = introspectedTable.getExampleType();
        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        String domainObjectName = StringUtils.dbStringToCamelStyle(tableName);

        JavaTypeResolver javaTypeResolver = new JavaTypeResolverDefaultImpl();
        FullyQualifiedJavaType calculateJavaType = javaTypeResolver
                .calculateJavaType(introspectedTable.getPrimaryKeyColumns().get(0));
        StringBuilder builder = new StringBuilder();
//        FullyQualifiedJavaType superInterfaceType = new FullyQualifiedJavaType(
//                builder.append("BaseService<")
//                       .append(entityClazzType)
//                        .append(",")
//                        .append(entityExampleClazzType)
//                        .append(",")
//                        .append(calculateJavaType.getShortName()).append(">").toString());

        Interface serviceInterface = new Interface(
                builder.delete(0, builder.length())
                        .append(serviceSuperPackage)
                        .append(".")
                        .append(domainObjectName)
                        .append("Service")
                        .toString()
        );

//        serviceInterface.addSuperInterface(superInterfaceType);
        serviceInterface.setVisibility(JavaVisibility.PUBLIC);

//        FullyQualifiedJavaType baseServiceInstance = FullyQualifiedJavaTypeProxyFactory.getBaseServiceInstance();
        FullyQualifiedJavaType modelJavaType = new FullyQualifiedJavaType(entityClazzType);
        FullyQualifiedJavaType exampleJavaType = new FullyQualifiedJavaType(entityExampleClazzType);
//        serviceInterface.addImportedType(baseServiceInstance);
        serviceInterface.addImportedType(modelJavaType);
        serviceInterface.addImportedType(exampleJavaType);
        serviceInterface.addFileCommentLine("/*** copyright (c) 2019 Marvis  ***/");


        this.additionalServiceMethods(introspectedTable, serviceInterface);
        return serviceInterface;
    }

    protected CompilationUnit addServiceImplClazz(IntrospectedTable introspectedTable, String targetPackage,
                                                  String implementationPackage) {

        String entityClazzType = introspectedTable.getBaseRecordType();
        String serviceSuperPackage = targetPackage;
        String serviceImplSuperPackage = implementationPackage;
        String entityExampleClazzType = introspectedTable.getExampleType();

        String javaMapperType = introspectedTable.getMyBatis3JavaMapperType();

//        String domainObjectName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        String domainObjectName = StringUtils.dbStringToCamelStyle(tableName);

        JavaTypeResolver javaTypeResolver = new JavaTypeResolverDefaultImpl();
        FullyQualifiedJavaType calculateJavaType = javaTypeResolver
                .calculateJavaType(introspectedTable.getPrimaryKeyColumns().get(0));

        StringBuilder builder = new StringBuilder();

        FullyQualifiedJavaType superClazzType = new FullyQualifiedJavaType(
                builder.append("BaseServiceImpl<")
                        .append(entityClazzType)
                        .append(",")
                        .append(entityExampleClazzType)
                        .append(",")
                        .append(calculateJavaType.getShortName()).append(">")
                        .toString()
        );

        FullyQualifiedJavaType implInterfaceType = new FullyQualifiedJavaType(

                builder.delete(0, builder.length())
                        .append(serviceSuperPackage)
                        .append(".")
                        .append(domainObjectName)
                        .append("Service")
                        .toString()
        );

        TopLevelClass serviceImplClazz = new TopLevelClass(

                builder.delete(0, builder.length())
                        .append(serviceImplSuperPackage)
                        .append(".")
                        .append(domainObjectName)
                        .append("ServiceImpl")
                        .toString()
        );

        serviceImplClazz.addSuperInterface(implInterfaceType);
//        serviceImplClazz.setSuperClass(superClazzType);
        serviceImplClazz.setVisibility(JavaVisibility.PUBLIC);
        serviceImplClazz.addAnnotation("@Service");

//        FullyQualifiedJavaType baseServiceInstance = FullyQualifiedJavaTypeProxyFactory.getBaseServiceImplInstance();
        FullyQualifiedJavaType modelJavaType = new FullyQualifiedJavaType(entityClazzType);
        FullyQualifiedJavaType exampleJavaType = new FullyQualifiedJavaType(entityExampleClazzType);
        serviceImplClazz
                .addImportedType(new FullyQualifiedJavaType("org.springframework.beans.factory.annotation.Autowired"));
        serviceImplClazz.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Service"));
//        serviceImplClazz.addImportedType(baseServiceInstance);
        serviceImplClazz.addImportedType(modelJavaType);
        serviceImplClazz.addImportedType(exampleJavaType);
        serviceImplClazz.addImportedType(implInterfaceType);

        FullyQualifiedJavaType logType = new FullyQualifiedJavaType("org.slf4j.Logger");
        FullyQualifiedJavaType logFactoryType = new FullyQualifiedJavaType("org.slf4j.LoggerFactory");
        Field logField = new Field("", logType);
        logField.setVisibility(JavaVisibility.PRIVATE);
        logField.setStatic(true);
        logField.setFinal(true);
        logField.setType(logType);
        logField.setName("logger");
        logField.setInitializationString(
                builder.delete(0, builder.length())
                        .append("LoggerFactory.getLogger(")
                        .append(domainObjectName)
                        .append("ServiceImpl.class)")
                        .toString()
        );

        logField.addAnnotation("");
        logField.addAnnotation("@SuppressWarnings(\"unused\")");
        serviceImplClazz.addField(logField);
        serviceImplClazz.addImportedType(logType);
        serviceImplClazz.addImportedType(logFactoryType);

        String mapperName = builder.delete(0, builder.length())
                .append(Character.toLowerCase(domainObjectName.charAt(0)))
                .append(domainObjectName.substring(1))
                .append("Mapper")
                .toString();

        FullyQualifiedJavaType JavaMapperType = new FullyQualifiedJavaType(javaMapperType);

        Field mapperField = new Field("", JavaMapperType);
        mapperField.setVisibility(JavaVisibility.PUBLIC);
        mapperField.setType(JavaMapperType);// Mapper.java
        mapperField.setName(mapperName);
        mapperField.addAnnotation("@Autowired");
        serviceImplClazz.addField(mapperField);
        serviceImplClazz.addImportedType(JavaMapperType);

        Method mapperMethod = new Method("");
        mapperMethod.setVisibility(JavaVisibility.PUBLIC);
        mapperMethod.setName("setMapper");
        mapperMethod.addBodyLine("super.setMapper(" + mapperName + ");");
        mapperMethod.addAnnotation("@Autowired");

        serviceImplClazz.addMethod(mapperMethod);
        serviceImplClazz.addFileCommentLine("/*** copyright (c) 2019 Marvis  ***/");

        serviceImplClazz
                .addImportedType(new FullyQualifiedJavaType("org.springframework.beans.factory.annotation.Autowired"));

        this.additionalServiceImplMethods(introspectedTable, serviceImplClazz, mapperName);

        return serviceImplClazz;
    }

    protected void additionalServiceMethods(IntrospectedTable introspectedTable, Interface serviceInterface) {
        if (this.notHasBLOBColumns(introspectedTable)) {
            return;
        }

//        introspectedTable.getGeneratedJavaFiles().stream().filter(file -> file.getCompilationUnit().isJavaInterface()
        introspectedTable.getGeneratedJavaFiles().stream().filter(file -> true
                && file.getCompilationUnit().getType().getShortName().endsWith("Mapper")).map(GeneratedJavaFile::getCompilationUnit).forEach(
                compilationUnit -> ((Interface) compilationUnit).getMethods().forEach(
                        m -> serviceInterface.addMethod(this.additionalServiceLayerMethod(serviceInterface, m))));
    }

    protected void additionalServiceImplMethods(IntrospectedTable introspectedTable, TopLevelClass clazz, String mapperName) {
        if (this.notHasBLOBColumns(introspectedTable)) {
            return;
        }
//        introspectedTable.getGeneratedJavaFiles().stream().filter(file -> file.getCompilationUnit().isJavaInterface()
        introspectedTable.getGeneratedJavaFiles().stream().filter(file -> false
                && file.getCompilationUnit().getType().getShortName().endsWith("Mapper")).map(GeneratedJavaFile::getCompilationUnit).forEach(
                compilationUnit -> ((Interface) compilationUnit).getMethods().forEach(m -> {
                    Method serviceImplMethod = this.additionalServiceLayerMethod(clazz, m);
                    serviceImplMethod.addAnnotation("@Override");
                    serviceImplMethod.addBodyLine(this.generateBodyForServiceImplMethod(mapperName, m));
                    clazz.addMethod(serviceImplMethod);
                }));
    }


    private boolean notHasBLOBColumns(IntrospectedTable introspectedTable) {
        return !introspectedTable.hasBLOBColumns();
    }

    private Method additionalServiceLayerMethod(CompilationUnit compilation, Method m) {
        Method method = new Method("");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setName(m.getName());
        List<Parameter> parameters = m.getParameters();
        method.getParameters().addAll(parameters.stream().peek(param -> param.getAnnotations().clear()).collect(Collectors.toList()));
        method.setReturnType((m.getReturnType()).get());
        compilation.addImportedType(
                new FullyQualifiedJavaType(m.getReturnType().get().getFullyQualifiedNameWithoutTypeParameters()));
        return method;
    }

    private String generateBodyForServiceImplMethod(String mapperName, Method m) {
        StringBuilder sbf = new StringBuilder("return ");
        sbf.append(mapperName).append(".").append(m.getName()).append("(");
        boolean singleParam = true;
        for (Parameter parameter : m.getParameters()) {
            if (singleParam) {
                singleParam = !singleParam;
            } else {
                sbf.append(", ");
            }
            sbf.append(parameter.getName());
        }
        sbf.append(");");
        return sbf.toString();
    }
}
