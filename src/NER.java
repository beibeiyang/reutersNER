/***********************************************************************
 * This program shows how to perform the Named Entity Recognition (NER)
 * using Stanford CoreNLP on the Reuters-21578 corpus.
 * 
 * Author: Beibei Yang (byang1@cs.uml.edu)
 * Last Modified: 12/18/2012
 * 
 * Given a text file as input (arg[0]), the program recognizes people names 
 * and locations, and counts their occurrences.
 * 
 * The input file reut2-000.sgm is an example of messy data:
 * 
 *  - DOCTYPE lewis.dtd does not fit the sgm data
 *  - Special characters such as "&#3;" gives org.xml.sax.SAXParseException
 *  - A mix of "<" and "&lt;"s in the BODY elements
 *  - Misspelled words, such as "Preesident Nixon"
 *  
 * This program is able to handle such messy data. 
 ***********************************************************************/

import java.io.*;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.util.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.*;
import org.w3c.dom.Document;

public class NER {
	public static void main(String[] args) throws IOException,
			ParserConfigurationException {

		long startTime = System.nanoTime();
		String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";

		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier
				.getClassifierNoExceptions(serializedClassifier);

		/*
		 * For either a file to annotate or for the hardcoded text example, the
		 * code below shows two ways to process the input. 
		 * For the file, it prints out all the recognized names and locations. 
		 * For the hard-coded String, it shows how to run it on a single sentence, 
		 * and how to do this and produce an inline XML output format.
		 */
		if (args.length > 0) {
			String fileContents = IOUtils.slurpFile(args[0]);
			try {
				// remove <!DOCTYPE lewis SYSTEM "lewis.dtd">
				fileContents = fileContents.replaceAll("<!DOCTYPE.*\">", "") ;
				// replace special characters such as &#3;
				fileContents = fileContents.replaceAll("&#[0-9]+;", "").trim() ;
				Document doc = XMLUtils.readDocumentFromString( "<ALLARTICLES>" + 
																fileContents + "</ALLARTICLES>");
				// get articles
				NodeList articles = doc.getElementsByTagName("BODY");
				for (int a = 0; a < articles.getLength(); a++) {

					System.out.println( a+1 + " of " + articles.getLength() + "..." );
					
					String content = articles.item(a).getTextContent();
					content = StringEscapeUtils.escapeHtml3(content);
					String annotatedText = classifier.classifyWithInlineXML(content);
					
					Map<String, Integer> nameEntities = new HashMap<String, Integer>();
					Map<String, Integer> locationEntities = new HashMap<String, Integer>();

					// person
					doc = XMLUtils.readDocumentFromString( "<NERCONTENT>" + annotatedText + "</NERCONTENT>");
					NodeList NERnl = doc.getElementsByTagName("PERSON");
					for (int i = 0; i < NERnl.getLength(); i++) {
						String name = NERnl.item(i).getTextContent();
						if (nameEntities.containsKey(name)) {
							nameEntities.put(name, nameEntities.get(name) + 1);
						} else {
							nameEntities.put(name, 1);
						}
					}
					
					// location
					doc = XMLUtils.readDocumentFromString( "<NERCONTENT>" + annotatedText + "</NERCONTENT>");
					NERnl = doc.getElementsByTagName("LOCATION");
					for (int i = 0; i < NERnl.getLength(); i++) {
						String name = NERnl.item(i).getTextContent();
						name = name.replaceAll("\n", "");
						if (locationEntities.containsKey(name)) {
							locationEntities.put(name, locationEntities.get(name) + 1);
						} else {
							locationEntities.put(name, 1);
						}
					}
					
					// print out recognized names
					System.out.println ( "Name(s):\t" + nameEntities.toString() );
					// print out recognized locations		
					System.out.println ( "Location(s):\t" + locationEntities.toString() );
					
					System.out.println ();
					
				}
		
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			String s1 = "Good afternoon Beibei Yang, how are you today?";
			String s2 = "I'm from EMC. Its headquarter is in Hopkinton MA USA.";
			System.out.println(classifier.classifyToString(s1));
			System.out.println(classifier.classifyToString(s2));
			System.out.println(classifier.classifyWithInlineXML(s1));
			System.out.println(classifier.classifyWithInlineXML(s2));
			System.out.println(classifier.classifyToString(s2, "xml", true));
		}
		
		// count elapsed time
		long endTime = System.nanoTime();
		// in nanoseconds
		long elapsedTime = endTime - startTime;	
		// convert to seconds
		double seconds = (double)elapsedTime / 1000000000.0;
		System.out.println ("Elapsed time: " + seconds + " seconds");
	}
}
