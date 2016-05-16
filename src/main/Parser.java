package main;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@SuppressWarnings("serial")
public class Parser extends JFrame implements ActionListener{
	public static String decoding = "ISO-8859-1";
	public static String encoding = "EUC-KR";
	private static final String naver_url = "http://music.naver.com";
	private static final String naver_search = "/search/search.nhn?query=";
	JButton button;
	
	public Parser(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("mp3 속성 파서");
		setSize(250, 100);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); // 모니터화면의
																		// 해상도
																		// 얻기
		Dimension f1_size = super.getSize(); // 프레임크기
		
		// 프레임이 화면 중앙에 위치하도록 left, top 계산
		int left = (screen.width / 2) - (f1_size.width / 2);
		int top = (screen.height / 2) - (f1_size.height / 2);
		setLocation(left, top);
		button = new JButton("\uD30C\uC77C \uC5F4\uAE30");
		getContentPane().add(button, BorderLayout.CENTER);
		button.addActionListener(this);
		setVisible(true);
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == button){
			
			
			FileDialog fd = new FileDialog(this, "파일 열기", FileDialog.LOAD);
			fd.setMultipleMode(true);
			fd.setVisible(true);
			
			File[] list = fd.getFiles();
			if(list.length > 0){
				
				new ParsingTask(list).start();
			}
			
		}
	}
	
	synchronized private String getHtml(String urlStr){
		URL url = null;
		try{
			url = new URL(urlStr);
		}catch(MalformedURLException e){
			e.printStackTrace();
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		String readLine = null;
		try{
			
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			
			while((readLine = br.readLine()) != null)
				sb.append(readLine);
			
			return sb.toString();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * 한글만 UTF-8로 인코딩
	 * 
	 */
	private String convertUrl(String url){
		String str = null;
		String result = "";
		
		for(Character ch : url.toCharArray()){
			str = ch.toString();
			
			if(isHangul(ch))
				try{
					str = URLEncoder.encode(str, "UTF-8");
				}catch(UnsupportedEncodingException e){
					e.printStackTrace();
				}
			
			result += str;
		}
		
		return result;
	}
	
	private boolean isHangul(char ch){
		UnicodeBlock block = UnicodeBlock.of(ch);
		
		if(UnicodeBlock.HANGUL_SYLLABLES == block || UnicodeBlock.HANGUL_JAMO == block || UnicodeBlock.HANGUL_COMPATIBILITY_JAMO == block)
			return true;
		
		return false;
	}
	
	//여러 mp3 파일을 입력 받아 속성 파싱
	private class ParsingTask extends Thread{
		private File[] fileList;
		private int max_count;
		private ShowProgress progress;
		File tmp, artwork;
		
		public ParsingTask(File[] list){
			progress = new ShowProgress(list.length);
			fileList = list;
			max_count = 5;
			artwork = null;
		}
		
		@Override
		public void run(){
			Tag tag;
			tmp = new File("tmp");
			tmp.mkdirs();
			progress.setVisible(true);
			
			// mp3 파일 각각 속성 파싱
			for(File file : fileList){
				try{
					file.setReadable(true);
					file.setWritable(true);
					
					String fileName = file.getName();
					
					//폴더이면
					if(file.isDirectory())
						continue;
					//mp3 파일이 아니면
					if(fileName.contains(".mp3") == false)
						continue;
					
					int delimIndex = fileName.indexOf('-');
					String artistName = fileName.substring(0, delimIndex).trim();
					String titleStr = fileName.substring(delimIndex + 1).trim().replace(".mp3", "");
					String titleExcept = titleStr;
					while(titleExcept.contains("(") && titleExcept.contains(")"))
						titleExcept = titleExcept.replace(titleExcept.substring(titleExcept.indexOf("("), titleExcept.indexOf(")")+1), "").trim();
					
					// 네이버로부터 mp3 정보 가져오기
					boolean isFound = false;
					String total_url = naver_url + naver_search + artistName.replaceAll(" ", "+") + "+" + titleStr
							.replaceAll(" ", "+");
					total_url += "&&target=track";
					String converted_url = convertUrl(total_url);
					String html = getHtml(converted_url);
					int count = max_count;
					while(html == null){
						html = getHtml(converted_url);
						
						//최대시도횟수를 초과하면
						if(count-- <= 0)
							throw new Exception("인터넷 접속 오류(최대 시도 횟수 초과: 5)");
					}
					
					//첫 화면에서 곡 검색
					Document doc = Jsoup.parse(html);
					Elements contents = doc.select("table tbody tr");
					Element nameElement = null;
					Element albumElement = null;
					String album_url = null;
					String albumName = null;
					String name = null;
					
					for(Element content : contents){
						nameElement = content.select("td.name").first();
						name = nameElement.text().toLowerCase();
						//mr 제외
						if(name.contains("inst.") || name.contains("instrument") || name.contains("mr")) 
							continue;
						
						//괄호 제거 후 비교
						while(name.contains("(") && name.contains(")"))
							name = name.replace(name.substring(name.indexOf("("), name.indexOf(")")+1),"").trim();
						
						if(name.equalsIgnoreCase(titleExcept))
								isFound = true;
							
						if(isFound){
							albumElement = content.select("td.album").first();
							album_url = albumElement.childNode(0).attr("href");
							albumName = albumElement.text();
							break;
						}
					}
					
					//다음 페이지에서 자세한 정보 검색
					String imgStr = null;
					String genreName = null;
					String yearStr = null;
					String trackStr = null;
					if(isFound){
						doc = Jsoup.parse(getHtml(naver_url + album_url));
						
						// 앨범 이미지 찾기
						contents = doc.select("div#content");
						imgStr = contents.first().child(0).child(0).child(0).child(0).child(0).attr("src");
						imgStr = imgStr.substring(0, imgStr.lastIndexOf("?type"));
						String imgName = imgStr.substring(imgStr.lastIndexOf("/")+1);
						URL imgUrl = new URL(imgStr);
						BufferedImage img = ImageIO.read(imgUrl);
						if(img != null){
							artwork = new File("tmp/"+imgName);
							if(artwork.createNewFile())
								ImageIO.write(img, imgName.substring(imgName.lastIndexOf(".")+1), artwork);
						}
						
						// 장르, 연도 찾기
						contents = doc.select("div.info_txt dl.desc");
						Element info = contents.first().child(3);
						genreName = info.text();
						yearStr = info.nextElementSibling().nextElementSibling().text();
						yearStr = yearStr.substring(0, yearStr.indexOf('.'));
						
						// 트랙 번호 찾기
						contents = doc.select("table tbody tr");
						for(Element content : contents){
							nameElement = content.select("td.name").first();
							if(nameElement.text().equalsIgnoreCase(name)){
								trackStr = content.select("td.order").first().text();
								break;
							}
						}
					}
					
					System.out.println(imgStr);
					System.out.println(titleStr);
					System.out.println(artistName);
					System.out.println(albumName);
					System.out.println(genreName);
					System.out.println(yearStr);
					System.out.println(trackStr);
					
					// 속성 적용
					MP3File mp3File = (MP3File) AudioFileIO.read(file);
					tag = mp3File.getTag();
					
					tag.setField(FieldKey.TITLE, new String(titleStr.getBytes(encoding), decoding));
					tag.setField(FieldKey.ARTIST, new String(artistName.getBytes(encoding), decoding));
					if(isFound){
						tag.deleteArtworkField();
						Artwork a = null;
						if(artwork != null)
							 a = ArtworkFactory.createArtworkFromFile(artwork);
						tag.setField(FieldKey.ALBUM, new String(albumName.getBytes(encoding), decoding));
						tag.setField(FieldKey.GENRE, new String(genreName.getBytes(encoding), decoding));
						tag.setField(FieldKey.YEAR, new String(yearStr.getBytes(encoding), decoding));
						tag.setField(FieldKey.TRACK, new String(trackStr.getBytes(encoding), decoding));
						if(a != null)
							tag.setField(a);
					}
					
					mp3File.setTag(tag);
//					AudioFileIO.write(mp3File);
					mp3File.commit();
					
					progress.appendLog(file.getName() + " 완료\n");
					
				}catch(Exception exception){
					progress.appendLog(file.getName() + ":" + exception.getMessage() + "\n");
					
				}finally{
					//임시 파일 삭제
					if(artwork != null && artwork.exists())
						artwork.delete();
					
					if(progress.progress())
						tmp.delete();
				}
			}
			
		}
	}
	
}
