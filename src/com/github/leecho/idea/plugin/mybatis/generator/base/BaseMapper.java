package com.github.leecho.idea.plugin.mybatis.generator.base;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * BaseMapper
 *
 * @param <T>
 * @param <Example>
 * @param <ID>
 */
public interface BaseMapper<T, Example, ID> {

    /**
     * @param example
     * @return
     */
    long countByExample(Example example);

    /**
     * @param example
     * @return
     */
    int deleteByExample(Example example);

    /**
     * @param id
     * @return
     */
    int deleteByPrimaryKey(ID id);

    /**
     * @param record
     * @return
     */
    int insert(T record);

    /**
     * @param record
     * @return
     */
    int insertSelective(T record);

    /**
     * @param example
     * @return
     */
    List<T> selectByExample(Example example);

    /**
     * @param id
     * @return
     */
    T selectByPrimaryKey(ID id);

    /**
     * @param record
     * @param example
     * @return
     */
    int updateByExampleSelective(@Param("record") T record, @Param("example") Example example);

    /**
     * @param record
     * @param example
     * @return
     */
    int updateByExample(@Param("record") T record, @Param("example") Example example);

    /**
     * @param record
     * @return
     */
    int updateByPrimaryKeySelective(T record);

    /**
     * @param record
     * @return
     */
    int updateByPrimaryKey(T record);

}