package main;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;


@SuppressWarnings("serial")
public class ShowProgress extends JFrame{
	private int total;
	private int current;
	private JProgressBar progressBar;
	private JTextArea textArea;
	
	public ShowProgress(int total){
		this();
		this.total = total;
		current = 0;
		progressBar.setMaximum(total);
		
	}
	
	private ShowProgress(){
		setTitle("�Ӽ� �Ľ� ����");
		setAlwaysOnTop(true);
		setSize(500, 250);
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); // �����ȭ���� �ػ� ���
		Dimension f1_size = super.getSize(); // ������ũ��
		// �������� ȭ�� �߾ӿ� ��ġ�ϵ��� left, top ���
		int left = (screen.width / 2) - (f1_size.width / 2);
		int top = (screen.height / 2) - (f1_size.height / 2);
		setLocation(left, top);
		getContentPane().setLayout(null);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(10, 25, 470, 15);
		getContentPane().add(progressBar);
		
		textArea = new JTextArea();
		
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBounds(10, 57, 470, 150);
		getContentPane().add(scrollPane);
		
	}
	
	public boolean progress(){
		progressBar.setValue(++current);
		
		if(current >= total){
			JOptionPane.showMessageDialog(ShowProgress.this, "�Ӽ� �Ľ� �Ϸ�");
			dispose();
			return true;
		}
		
		return false;
	}
	
	public void appendLog(String log){
		textArea.append(log);
		textArea.setCaretPosition(textArea.getDocument().getLength());
		
	}
}
