package com.github.leecho.idea.plugin.mybatis.generator.model;

/**
 * todo
 */
public enum DbType {

    /**
     * MySQL连接信息
     */
    MySQL("com.mysql.jdbc.Driver", "jdbc:mysql://%s:%s/%s?useUnicode=true&useSSL=false&characterEncoding=%s", "mysql-connector-java-5.1.38.jar"),
    /**
     * MySQL 8 连接信息
     */
    MySQL_8("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useUnicode=true&useSSL=false&characterEncoding=%s", "mysql-connector-java-8.0.11.jar"),
    /**
     * Oracle 连接信息
     */
    Oracle("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@%s:%s:%s", "ojdbc14.jar"),
    /**
     * PostgreSQL 连接信息
     */
    PostgreSQL("org.postgresql.Driver", "jdbc:postgresql://%s:%s/%s", "postgresql-9.4.1209.jar"),
    /**
     * SqlServer 连接信息
     */
    SqlServer("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%s;databaseName=%s", "sqljdbc4-4.0.jar"),
    /**
     * Sqlite 连接信息
     */
    Sqlite("org.sqlite.JDBC", "jdbc:sqlite:%s", "sqlite-jdbc-3.19.3.jar"),
    /**
     * MariaDB 连接信息
     */
    MariaDB("org.mariadb.jdbc.Driver", "", "mariadb-java-client-2.3.0.jar");

    private final String driverClass;
    private final String connectionUrlPattern;
    private final String connectorJarFile;

    /**
     * todo
     * @param driverClass 驱动类
     * @param connectionUrlPattern 连接字符串
     * @param connectorJarFile 驱动包
     */
    DbType(String driverClass, String connectionUrlPattern, String connectorJarFile) {
        this.driverClass = driverClass;
        this.connectionUrlPattern = connectionUrlPattern;
        this.connectorJarFile = connectorJarFile;
    }

    /**
     * todo
     * @return 返回驱动类
     */
    public String getDriverClass() {
        return driverClass;
    }

    /**
     * todo
     * @return 返回连接字符串
     */
    public String getConnectionUrlPattern() {
        return connectionUrlPattern;
    }

    /**
     * todo
     * @return 返回驱动包
     */
    public String getConnectorJarFile() {
        return connectorJarFile;
    }
}