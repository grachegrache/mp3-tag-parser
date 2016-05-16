package main;
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

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//���� mp3 ������ �Է� �޾� �Ӽ� �Ľ�
public class Parser extends Thread{
	public static String decoding = "ISO-8859-1";
	public static String encoding = "EUC-KR";
	private final String naver_search = "/search/search.nhn?query=";
	private final String naver_lyric = "/lyric/index.nhn?trackId=";
	private final String naver_url = "http://music.naver.com";
	private File[] fileList;
	private int max_count;
	private ShowProgress progress;
	private File tmp, artwork;
	
	public Parser(File[] list){
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
		
		// mp3 ���� ���� �Ӽ� �Ľ�
		for(File file : fileList){
			try{
				file.setReadable(true);
				file.setWritable(true);
				
				String fileName = file.getName();
				
				//�����̸�
				if(file.isDirectory()){
					throw new Exception("�ش� ������ �����Դϴ�.");
				}
//					//mp3 ������ �ƴϸ�
//					if(fileName.contains(".mp3") == false){
//						throw new Exception("�ش� ������ mp3������ �ƴմϴ�.");
//					}
				
				int delimIndex = fileName.indexOf('-');
				String artistName = fileName.substring(0, delimIndex).trim();
				//-�� �߽����� ���� ����
				String titleStr = fileName.substring(delimIndex + 1).trim();
				//Ȯ���� ����
				titleStr = titleStr.substring(0, titleStr.lastIndexOf('.'));
				titleStr = exceptBracket(titleStr);
				
				// ���̹��κ��� ���� ���� ��������
				boolean isFound = false;
				String total_url = naver_url + naver_search + artistName.replaceAll(" ", "+") + "+" + titleStr
						.replaceAll(" ", "+");
				total_url += "&&target=track";
				String converted_url = convertUrl(total_url);
				String html = getHtml(converted_url);
				int count = max_count;
				while(html == null){
					html = getHtml(converted_url);
					
					//�ִ�õ�Ƚ���� �ʰ��ϸ�
					if(count-- <= 0)
						throw new Exception("���ͳ� ���� ����(�ִ� �õ� Ƚ�� �ʰ�: 5)");
				}
				
				//ù ȭ�鿡�� �� �˻�
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
					//mr ����
					if(name.contains("inst.") || name.contains("instrument") || name.contains("mr")) 
						continue;
					
					//��ȣ ���� �� ��
					name = exceptBracket(name);
					
					if(name.equalsIgnoreCase(titleStr))
							isFound = true;
						
					if(isFound){
						albumElement = content.select("td.album").first();
						album_url = albumElement.childNode(0).attr("href");
						albumName = albumElement.text();
						break;
					}
				}
				
				//���� ���������� �ڼ��� ���� �˻�
				String imgStr = null;
				String genreName = null;
				String yearStr = null;
				String trackStr = null;
				String trackidStr = null;
				String lyricStr = null;
				if(isFound){
					doc = Jsoup.parse(getHtml(naver_url + album_url));
					
					// �ٹ� �̹��� ã��
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
					
					// �帣, ���� ã��
					contents = doc.select("div.info_txt dl.desc");
					Element info = contents.first().child(3);
					genreName = info.text();
					yearStr = info.nextElementSibling().nextElementSibling().text();
					yearStr = yearStr.substring(0, yearStr.indexOf('.'));
					
					// Ʈ�� ��ȣ, ���� ã��
					contents = doc.select("table tbody tr");
					for(Element content : contents){
						nameElement = content.select("td.name").first();
						if(exceptBracket(nameElement.text()).equalsIgnoreCase(name)){
							trackStr = content.select("td.order").first().text();
							trackidStr = content.attr("trackdata");
							trackidStr = trackidStr.substring(0, trackidStr.indexOf('|'));
							
							if(trackidStr != null){
								doc = Jsoup.parse(getHtml(naver_url+naver_lyric+trackidStr));
								contents = doc.select("div#lyricText");
								lyricStr = contents.first().html();
								lyricStr = lyricStr.replaceAll("<br>", "\n");
								lyricStr = lyricStr.replaceAll("<br/>", "\n");
							}
							
							break;
						}
					}
				}
				
//				System.out.println(imgStr);
//				System.out.println(titleStr);
//				System.out.println(artistName);
//				System.out.println(albumName);
//				System.out.println(genreName);
//				System.out.println(yearStr);
//				System.out.println(trackStr);
//				System.out.println(lyricStr);
				
				// �Ӽ� ����
				AudioFile audio = AudioFileIO.read(file);
				tag = audio.getTagOrCreateAndSetDefault();
				
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
					tag.setField(FieldKey.LYRICS, new String(lyricStr.getBytes(encoding), decoding));
				}
				
//				audio.setTag(tag);
				audio.commit();
//				AudioFileIO.write(audio);
				
				progress.appendLog(file.getName() + " �Ϸ�\n");
				
			}catch(Exception exception){
				progress.appendLog(file.getName() + ":" + exception.getMessage() + "\n");
				
			}finally{
				//�ӽ� ���� ����
				if(artwork != null && artwork.exists())
					artwork.delete();
				
				if(progress.progress())
					tmp.delete();
			}
		}
		
	}
	
	private String exceptBracket(String str){
		while(str.contains("(") && str.contains(")"))
			str = str.replace(str.substring(str.indexOf("("), str.indexOf(")")+1), "").trim();
		
		return str;
	}
	
	/*
	 * �ѱ۸� UTF-8�� ���ڵ�
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

	private boolean isHangul(char ch){
		UnicodeBlock block = UnicodeBlock.of(ch);
		
		if(UnicodeBlock.HANGUL_SYLLABLES == block || UnicodeBlock.HANGUL_JAMO == block || UnicodeBlock.HANGUL_COMPATIBILITY_JAMO == block)
			return true;
		
		return false;
	}
	
}
