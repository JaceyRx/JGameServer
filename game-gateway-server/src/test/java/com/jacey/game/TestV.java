package com.jacey.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UnsupportedEncodingException;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class TestV extends JFrame {
    public TestV() {
        initComponents();
        this.setVisible(true);
        //窗口关闭时
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel palyerName_lab;
    private JLabel password_lab;
    private JTextField palyerName_text;
    private JPasswordField password_text;
    private JButton registered_btn;
    private JButton login_btn;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        palyerName_lab = new JLabel();
        password_lab = new JLabel();
        palyerName_text = new JTextField();
        password_text = new JPasswordField();
        registered_btn = new JButton();
        login_btn = new JButton();

        //======== this ========
        setTitle("[Tic-Tac-Toe]-ByJacey");
        setResizable(false);
        Container contentPane = getContentPane();

        //---- palyerName_lab ----
        palyerName_lab.setText("\u7528\u6237\u540d");

        //---- password_lab ----
        password_lab.setText("\u5bc6 \u7801\uff1a");

        //---- registered_btn ----
        registered_btn.setText("\u6ce8\u518c");
        registered_btn.addMouseListener(new MouseAdapter() {
            // 登录按钮事件监听
            @Override
            public void mouseClicked(MouseEvent e) {
                registered_btnMouseClicked(e);
            }
        });

        //---- login_btn ----
        login_btn.setText("\u767b\u5f55");
        // 登录按钮事件监听
        login_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                login_btnMouseClicked(e);
            }
        });

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(password_lab, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                        .addComponent(palyerName_lab, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                .addComponent(registered_btn)
                                                .addGap(25, 25, 25)
                                                .addComponent(login_btn))
                                        .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addComponent(password_text, GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                                                .addComponent(palyerName_text, GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)))
                                .addGap(36, 36, 36))
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGap(43, 43, 43)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(palyerName_lab, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(palyerName_text, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(26, 26, 26)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(password_lab, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(password_text, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(login_btn)
                                        .addComponent(registered_btn))
                                .addContainerGap(22, Short.MAX_VALUE))
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents

    }

    public static void main(String[] args) {
        new TestV();
    }


    /**
     * 注册按钮。鼠标点击事件处理
     * @param e
     */
    private void registered_btnMouseClicked(MouseEvent e) {

    }

    /**
     * 登录按钮。鼠标点击事件处理
     * @param e
     */
    private void login_btnMouseClicked(MouseEvent e)  {
            String str = "未连接服务器.无法登录. 服务器地址HOST:";
            byte[] temp= new byte[0];
            try {
                temp = str.getBytes("gbk");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            str=new String(temp);
            JOptionPane.showMessageDialog(null, str);
    }
}
