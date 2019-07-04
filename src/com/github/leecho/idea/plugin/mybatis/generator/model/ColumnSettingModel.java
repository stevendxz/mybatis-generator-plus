package com.github.leecho.idea.plugin.mybatis.generator.model;


import com.intellij.openapi.ui.Messages;

/**
 * 类型隐射模型
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class ColumnSettingModel extends AbstractTableModel<ColumnSetting> {
	@Override
	protected String[] initColumnName() {
		return new String[]{"Column", "Jdbc Type", "Java Property", "Java Type", "Ignore", "Comment"};
	}

	@Override
	protected Object[] toObj(ColumnSetting entity) {
		return new Object[]{entity.getColumn(), entity.getJdbcType(), entity.getJavaProperty(), entity.getJavaType(), entity.getIgnore(), entity.getComment()};
	}

	@Override
	protected boolean setVal(ColumnSetting setting, int columnIndex, Object val) {
		if (val == null || String.valueOf(val).length() == 0 || "".equals(String.valueOf(val))) {
			Messages.showMessageDialog("The value must not be null", "Mybatis Generator Plus", Messages.getWarningIcon());
			return false;
		}
		if (columnIndex == 0) {
			setting.setColumn((String) val);
		} else if (columnIndex == 1) {
			if (setting.getJdbcType().equals(val)) {
				return false;
			}
			setting.setJdbcType((String) val);
		} else if (columnIndex == 2) {
			if (setting.getJavaProperty().equals(val)) {
				return false;
			}
			setting.setJavaProperty((String) val);
		} else if (columnIndex == 3) {
			if (setting.getJavaType().equals(val)) {
				return false;
			}
			setting.setJavaType((String) val);
		} else if (columnIndex == 4) {
			setting.setIgnore((Boolean) val);
		}
		setting.setChanged(true);
		return true;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return column != 0 && column != 5;
	}
}
