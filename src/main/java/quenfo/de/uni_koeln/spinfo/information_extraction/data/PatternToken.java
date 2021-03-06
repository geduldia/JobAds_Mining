package quenfo.de.uni_koeln.spinfo.information_extraction.data;

/**
 * @author geduldia
 * 
 * Represents a single Token as part of an Extraction-Pattern.
 * The attributes string, lemma and posTag can be null, if values are not specified in the pattern
 *
 */
public class PatternToken extends Token {

	
	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 */
	public PatternToken(String string, String lemma, String posTag) {
		super(string, lemma, posTag);
	}
	
	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 * @param isInformationEntity
	 */
	public PatternToken(String string, String lemma, String posTag, boolean isInformationEntity) {
		super(string, lemma, posTag, isInformationEntity);
	}
	
	public void setLemma(String lemma){
		this.lemma = lemma;
	}
	
	public void setPosTag(String posTag){
		this.posTag = posTag;
	}
	public void setString(String string){
		this.string = string;
	}
	
	public String getLemma(){
		if(isModifier()) return "IMPORTANCE";
		return this.lemma;
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(string + "\t" + lemma + "\t" + posTag + "\t");
		if(this.ieToken){
			sb.append("isInformsationEntitiy"+"\t");
		}
		if(this.modifierToken){
			sb.append("is (start of) modifier"+"\t");
		}
		return sb.toString();
	}
}
