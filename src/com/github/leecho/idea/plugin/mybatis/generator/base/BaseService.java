package com.github.leecho.idea.plugin.mybatis.generator.base;

import java.util.List;

import com.github.leecho.idea.plugin.mybatis.generator.model.PageInfo;
import org.apache.ibatis.annotations.Param;

public interface BaseService<T, Example extends BaseExample, ID> {

    long countByExample(Example example);

    int deleteByExample(Example example);

    int deleteByPrimaryKey(ID id);

    int insert(T record);

    int insertSelective(T record);

    List<T> selectByExample(Example example);
    
    /**
     * return T object
     * @author Marvis
     * @date May 23, 2018 11:37:11 AM
     * @param example
     * @return
     */
    T selectByCondition(Example example);
    /**
     * if pageInfo == null<p/>
     * then return result of selectByExample(example)
     * @author Marvis
     * @date Jul 13, 2017 5:24:35 PM
     * @param example
     * @param pageInfo
     * @return
     */
    List<T> selectByPageExmple(Example example, PageInfo pageInfo);

    T selectByPrimaryKey(ID id);

    int updateByExampleSelective(@Param("record") T record, @Param("example") Example example);

    int updateByExample(@Param("record") T record, @Param("example") Example example);

    int updateByPrimaryKeySelective(T record);

    int updateByPrimaryKey(T record);
}
