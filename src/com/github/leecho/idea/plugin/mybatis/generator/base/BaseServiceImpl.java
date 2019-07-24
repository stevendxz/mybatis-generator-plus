package com.github.leecho.idea.plugin.mybatis.generator.base;

import com.github.leecho.idea.plugin.mybatis.generator.model.PageInfo;

import java.util.List;

public abstract class BaseServiceImpl<T, Example extends BaseExample, ID> implements BaseService<T, Example, ID> {

    private BaseMapper<T, Example, ID> mapper;

    public void setMapper(BaseMapper<T, Example, ID> mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public long countByExample(Example example) {
        return mapper.countByExample(example);
    }

    @Override
    public int deleteByExample(Example example) {
        return mapper.deleteByExample(example);
    }

    @Override
    public int deleteByPrimaryKey(ID id) {
        return mapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(T record) {
        return mapper.insert(record);
    }

    @Override
    public int insertSelective(T record) {
        return mapper.insertSelective(record);
    }

    @Override
    public List<T> selectByExample(Example example) {
        return mapper.selectByExample(example);
    }
    @Override
    public T selectByCondition(Example example) {
        
        List<T> datas = selectByExample(example);
        return datas != null && datas.size() == 0 ? null : datas.get(0);
    }

    @Override
    public List<T> selectByPageExmple(Example example, PageInfo pageInfo) {
        
        if(pageInfo != null){
            
            example.setPageInfo(pageInfo);
            pageInfo.setPageParams(Long.valueOf(this.countByExample(example)).intValue());
        }
        return this.selectByExample(example);
    }

    @Override
    public T selectByPrimaryKey(ID id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByExampleSelective(T record, Example example) {
        return mapper.updateByExampleSelective(record, example);
    }

    @Override
    public int updateByExample(T record, Example example) {
        return mapper.updateByExample(record, example);
    }

    @Override
    public int updateByPrimaryKeySelective(T record) {
        return mapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(T record) {
        return mapper.updateByPrimaryKey(record);
    }
}