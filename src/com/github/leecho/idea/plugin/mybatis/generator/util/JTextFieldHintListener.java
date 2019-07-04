package com.github.leecho.idea.plugin.mybatis.generator.util;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
/**
 * 输入框提示
 * Created by kangtian on 2018/8/3.
 */
public class JTextFieldHintListener implements FocusListener {
    private String hintText;
    private JTextField textField;
    public JTextFieldHintListener(JTextField jTextField,String hintText) {
        this.textField = jTextField;
        this.hintText = hintText;
        //默认直接显示
        jTextField.setText(hintText);
        jTextField.setForeground(Color.GRAY);
    }

    @Override
    public void focusGained(FocusEvent e) {
        //获取焦点时，清空提示内容
        String temp = textField.getText();
        if(temp.equals(hintText)) {
            textField.setText("");
            textField.setForeground(Color.BLACK);
        }

    }

    @Override
    public void focusLost(FocusEvent e) {
        //失去焦点时，没有输入内容，显示提示内容
        String temp = textField.getText();
        if("".equals(temp)) {
            textField.setForeground(Color.GRAY);
            textField.setText(hintText);
        }

    }

}
