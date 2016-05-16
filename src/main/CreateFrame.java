package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;


@SuppressWarnings("serial")
public class CreateFrame extends JFrame implements ActionListener,DropTargetListener{
	DropTarget dt;
	JButton button;
	
	public CreateFrame(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setAlwaysOnTop(true);
		setTitle("mp3 속성 파서");
		setSize(250, 120);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); // 모니터화면의 해상도 얻기
		Dimension f1_size = super.getSize(); // 프레임크기
		
		// 프레임이 화면 중앙에 위치하도록 left, top 계산
		int left = (screen.width / 2) - (f1_size.width / 2);
		int top = (screen.height / 2) - (f1_size.height / 2);
		setLocation(left, top);
		button = new JButton("\uD30C\uC77C \uC5F4\uAE30");
		getContentPane().add(button, BorderLayout.CENTER);
		button.addActionListener(this);
		
		dt = new DropTarget(button, this);
		
		setVisible(true);
		
	}
	
	private void startParsing(File[] list){
		if(list.length > 0){
			new Parser(list).start();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == button){
			
			FileDialog fd = new FileDialog(this, "파일 열기", FileDialog.LOAD);
			fd.setMultipleMode(true);
			fd.setVisible(true);
			
			File[] list = fd.getFiles();
			startParsing(list);
			
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde){
	}

	@Override
	public void dragExit(DropTargetEvent dte){
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde){
	}

	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent dtde){
		
		try{
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
			File[] list = (File[]) droppedFiles.toArray();
			startParsing(list);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde){
	}
}
