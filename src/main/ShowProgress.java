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
		setTitle("속성 파싱 진행");
		setAlwaysOnTop(true);
		setSize(500, 250);
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); // 모니터화면의 해상도 얻기
		Dimension f1_size = super.getSize(); // 프레임크기
		// 프레임이 화면 중앙에 위치하도록 left, top 계산
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
			JOptionPane.showMessageDialog(ShowProgress.this, "속성 파싱 완료");
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
