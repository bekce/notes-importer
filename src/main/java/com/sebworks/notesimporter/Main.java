package com.sebworks.notesimporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Creates apple notes app formated notes from plain note files. <br>
 * Input: a directory of text files <br>
 * Output: a directory of eml files which should be put in Notes directory using
 * a email client like Thunderbird.
 * 
 * @author seb
 * @since 09.01.2015
 *
 */
public class Main {

	static String INPUT_DATE_FORMAT;
	static String NOTE_ENCODING;
	static String DATEMAP_ENCODING;
	static Logger logger = Logger.getLogger(Main.class);
	public static void main(String[] args) {
		BasicConfigurator.configure();
		INPUT_DATE_FORMAT = System.getProperty("input.date.format", "yyyyMMdd-HHmmss");
		NOTE_ENCODING = System.getProperty("note.encoding", "utf-8");
		DATEMAP_ENCODING = System.getProperty("datemap.encoding", "utf-8");
				
		if(args.length != 4){
			logger.info("Usage: Main <sourcefolder> <targetfolder> <datemapfile> <emailAddress>");
			return;
		}
		
		try {
			new Main(args[0], args[1], args[2], args[3]);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	class Note {
		String filename;
		String content; 
		Date date;
		String subject(){
			String sub = content;
			int idx = sub.indexOf('\n');
			if(idx>0) sub = sub.substring(0, idx);
			sub = sub.replaceAll("\\p{Cntrl}", "").replaceAll("�","");
			return sub.substring(0, sub.length()>50 ? 50 : sub.length());
		}
		String content(){
			String sub = content.replaceAll("(?!\n)(\\p{Cntrl})", "").replaceAll("�","");
			return Utils.escapeToHtml(sub);
		}
		String dateString(){
			if(date == null) return null;
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
			return sdf.format(date);
		}
	}
	
	Main(final String sourcePath, final String targetPath,
			final String dateMapFilePath, final String emailAddress)
			throws IOException {
		Charset datemapCharset = Charset.forName(DATEMAP_ENCODING);
		Charset noteCharset = Charset.forName(NOTE_ENCODING);
		final String domain = emailAddress.substring(emailAddress.indexOf('@')+1);
		final List<Note> notes = new LinkedList<Main.Note>();
		
		//read dates
		final Map<String, Date> fileDates = new HashMap<String, Date>();
		final SimpleDateFormat sdf = new SimpleDateFormat(INPUT_DATE_FORMAT);
		List<String> readAllLines = Files.readAllLines(new File(dateMapFilePath).toPath(), datemapCharset);
		int lineNo = 0;
		for (String line : readAllLines) {
			lineNo++;
			line = line.trim();
			if(line.isEmpty()) continue;
			try {
				//line format: <date> <filename>
				Date date = sdf.parse(line.substring(0, line.indexOf(';')));
				String filename = line.substring(line.indexOf(';')+1);
				fileDates.put(filename, date);
			} catch (Exception e) {
				throw new IllegalArgumentException("Date map file line "+lineNo+" is illegal. Proper format for each line: <date("+INPUT_DATE_FORMAT+")>;<filename>");
			}
		}
		
		//read files
		File sourceDir = new File(sourcePath);
		for(File source: sourceDir.listFiles()){
			Note note = new Note();
			note.content = new String(Files.readAllBytes(source.toPath()), noteCharset);
			note.date = fileDates.get(source.getName());
			note.filename = source.getName();
			if(note.date == null){
				logger.warn("No date found for file '"+source.getName()+"', using last modified date on the file. ");
				note.date = new Date(source.lastModified());
			}
			notes.add(note);
		}
		
		//write files
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		/*  next, get the Template  */
		Template t = ve.getTemplate( "template.vm" );
		/*  create a context and add data */
		for (Note note : notes) {
			String messageId = String.format("<%s@%s>", UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH), domain);
			String uuId = UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH);
			String outputFileName = targetPath+File.separator+note.filename+".eml";

			VelocityContext context = new VelocityContext();
			context.put("subject", note.subject());
			context.put("from", emailAddress);
			context.put("messageId", messageId);
			context.put("uuId", uuId);
			context.put("content", note.content());
			context.put("date", note.dateString());

			FileWriter writer = new FileWriter(new File(outputFileName));
			t.merge( context, writer );
			writer.close();
			logger.info("OK: "+outputFileName);
		}
		logger.info("DONE");

	}

}
