package quenfo.de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
 * 
 *         workflow to match the not validated competence-extractions (output of
 *         the app.: ExtractNewCompetences) against all class 3 paragraphs
 *         
 *         input: all as class 3 (= applicants profile) classified paragraphs
 *         output: all matching 'competences' together with their containing sentence
 *
 */
public class MatchNotValidatedCompetences {


	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paragraphsDB =  "src/main/resources/classification/output/ClassifiedParagraphs.db";

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String outputFolder =  "src/main/resources/information_extraction/output/competences/";

	// Name der Output-DB
	static String outputDB = "NotValidatedCompetenceMatches.db";

	// DB mit den extrahierten Kompetenz-Vorschlägen
	static String extratedCompsDB =  "src/main/resources/information_extraction/output/competences/ExtractedCompetences.db";

	// txt-File mit den Modifizierern
	static File modifier = new File("src/main/resources/information_extraction/input/competences/modifier.txt");

	// txt-File zum Speichern der Match-Statistik
	static File statisticsFile = new File("src/main/resources/information_extraction/output/competences/notValidatedMatchingStats.txt");

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int maxCount = -1;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos = 0;

	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
		// Verbindung mit Input-DB
		Connection inputConnection = null;
		if (!new File(paragraphsDB).exists()) {
			System.out.println(
					"Database don't exists " + paragraphsDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(paragraphsDB);
		}
		// Verbindung mit Output-DB
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		Connection outputConnection = IE_DBConnector.connect(outputFolder + outputDB);
		IE_DBConnector.createExtractionOutputTable(outputConnection, IEType.COMPETENCE, false);

		// Prüfe ob maxCount und startPos gültige Werte haben
		String query = "SELECT COUNT(*) FROM ClassifiedParagraphs;";
		Statement stmt = inputConnection.createStatement();
		ResultSet countResult = stmt.executeQuery(query);
		int tableSize = countResult.getInt(1);
		stmt.close();
		if (tableSize <= startPos) {
			System.out.println("startPosition (" + startPos + ")is greater than tablesize (" + tableSize + ")");
			System.out.println("please select a new startPosition and try again");
			System.exit(0);
		}
		if (maxCount > tableSize - startPos) {
			maxCount = tableSize - startPos;
		}

		// Einlesen der extrahierten Kompetenzvorschläge
		System.out.println("read not validated Competences from DB");
		Connection extractionsConnection = IE_DBConnector.connect(extratedCompsDB);
		Set<String> extractions = IE_DBConnector.readEntities(extractionsConnection, IEType.COMPETENCE);
		// Kompetenz-Vorschläge in eine txt-Datei schreiben
		// (Der Umweg über den txt-File wird genommen, um den bereits
		// bestehenden Workflow zum Matchen der validierten Kompetenzen nutzen
		// zu können)
		File notValidatedCompetences = new File("src/main/resources/information_extraction/output/competences/notValidatedCompetences.txt");
		if(!notValidatedCompetences.exists()){
			notValidatedCompetences.createNewFile();
		}
		PrintWriter out = new PrintWriter(new FileWriter(notValidatedCompetences));
		for (String extracted : extractions) {
			out.write("\n" + extracted);
		}
		out.close();
		// start Matching
		long before = System.currentTimeMillis();
		// erzeugt einen Index auf die Spalte 'ClassTHREE' (falls noch nicht
		// vorhanden)
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTHREE");
		Extractor extractor = new Extractor(notValidatedCompetences, null, modifier, IEType.COMPETENCE, null);
		extractor.stringMatch(statisticsFile, inputConnection, outputConnection, maxCount, startPos);
		notValidatedCompetences.delete();
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished matching in " + (time / 60) + " hours");
		} else {
			System.out.println("\nfinished matching in " + time + " minutes");
		}

	}

}
