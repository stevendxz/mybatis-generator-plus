package com.github.leecho.idea.plugin.mybatis.generator.generate;

import cn.kt.DbRemarksCommentGenerator;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * Create with IntelliJ IDEA
 *
 * @ProjectName: mybatis-generator-plus
 * @ClassName: CustomCommentGenerator
 * @Description: TODO
 * @Date: 2019/7/4 17:20
 * @Author: Adtis-D
 * @Version: 1.0.0
 */
public class CustomCommentGenerator extends DbRemarksCommentGenerator {
    private boolean columnRemarks;
    private boolean isAnnotations;
    private String author;
    private String createTime;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public CustomCommentGenerator() {
    }

    @Override
    public void addJavaFileComment(CompilationUnit compilationUnit) {
        if (this.isAnnotations) {
            compilationUnit.addImportedType(new FullyQualifiedJavaType("javax.persistence.Table"));
            compilationUnit.addImportedType(new FullyQualifiedJavaType("javax.persistence.Id"));
            compilationUnit.addImportedType(new FullyQualifiedJavaType("javax.persistence.Column"));
            compilationUnit.addImportedType(new FullyQualifiedJavaType("javax.persistence.GeneratedValue"));
            compilationUnit.addImportedType(new FullyQualifiedJavaType("org.hibernate.validator.constraints.NotEmpty"));
        }

    }
    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable) {

    }
    @Override
    public void addModelClassComment(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * Create with Intellij IDEA" );
        topLevelClass.addJavaDocLine(" * @ClassName: " + introspectedTable.getFullyQualifiedTable().getDomainObjectName());
        topLevelClass.addJavaDocLine(" * @Description: TODO");
        topLevelClass.addJavaDocLine(" * @Date:  " );
        topLevelClass.addJavaDocLine(" * @Author: ");
        topLevelClass.addJavaDocLine(" * @Table: " + introspectedTable.getFullyQualifiedTable().getIntrospectedTableName());
        topLevelClass.addJavaDocLine(" * @Version: 1.0.0");
        topLevelClass.addJavaDocLine(" */");
        if (this.isAnnotations) {
            topLevelClass.addAnnotation("@Table(name=\"" + introspectedTable.getFullyQualifiedTableNameAtRuntime() + "\")");
        }

    }
}
