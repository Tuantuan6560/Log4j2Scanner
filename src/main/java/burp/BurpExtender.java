package burp;


import burp.UI.Log4jScanner;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;


public class BurpExtender implements IBurpExtender,ITab {

    private PrintWriter stdout;
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private Log4jScanner scanner;
    private JPanel panel;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.stdout = new PrintWriter(callbacks.getStdout(),true);
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        callbacks.setExtensionName("Red256's BpExtender");
        this.stdout.println("Log4j2Scanner is successful loaded");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scanner = new Log4jScanner(callbacks,helpers);
                panel = scanner.getScannerUI(callbacks);

                callbacks.customizeUiComponent(panel);
                callbacks.addSuiteTab(BurpExtender.this);
            }
        });
    }


    @Override
    public String getTabCaption() {
        return "Red256's log4jScanner";
    }

    @Override
    //该方法获得对应的UI界面
    public Component getUiComponent() {
        return panel;
    }
}
