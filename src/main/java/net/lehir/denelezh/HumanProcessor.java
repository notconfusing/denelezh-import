package net.lehir.denelezh;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public class HumanProcessor implements EntityDocumentProcessor {

    private BufferedWriter humanBW;
    private BufferedWriter humanCountryBW;
    private BufferedWriter humanOccupationBW;
    private BufferedWriter humanSiteLinkBW;
    private BufferedWriter labelBW;

    public static final String PROPERTY_INSTANCE_OF = "P31";
    public static final String PROPERTY_SUBCLASS_OF = "P279";
    public static final String PROPERTY_GENDER = "P21";
    public static final String PROPERTY_DATE_OF_BIRTH = "P569";
    public static final String PROPERTY_COUNTRY_OF_CITIZENSHIP = "P27";
    public static final String PROPERTY_OCCUPATION = "P106";

    public static final String ITEM_HUMAN = "Q5";
    public static final String ITEM_SOVEREIGN_STATE = "Q3624078";
    public static final String ITEM_COUNTRY = "Q6256";
    public static final String ITEM_OCCUPATION = "Q12737077";
    public static final String ITEM_GENDER_IDENTITY = "Q48264";
    public static final String ITEM_SEX_OF_HUMANS = "Q4369513";
    //array of strings
    public static final String[] itemLabelArray = {ITEM_HUMAN, ITEM_OCCUPATION, ITEM_GENDER_IDENTITY, ITEM_SEX_OF_HUMANS, ITEM_COUNTRY, ITEM_SOVEREIGN_STATE};
    //initialize an immutable list from array using asList method
    public static final List<String> LABEL_ITEMS = Arrays.asList(itemLabelArray);
    private int humansCount = 0;
    private String envMaxHumans = System.getenv("HUMANIKI_MAX_HUMANS");
    private Integer maxHumansToProcess = envMaxHumans != null? Integer.parseInt(envMaxHumans) : null;

    public HumanProcessor(BufferedWriter humanBW, BufferedWriter humanCountryBW, BufferedWriter humanOccupationBW, BufferedWriter humanSiteLinkBW, BufferedWriter labelBW) {
        this.humanBW = humanBW;
        this.humanCountryBW = humanCountryBW;
        this.humanOccupationBW = humanOccupationBW;
        this.humanSiteLinkBW = humanSiteLinkBW;
        this.labelBW = labelBW;
    }

    @Override
    public void processItemDocument(ItemDocument itemDocument) {

        Map<String, Map<StatementRank, Set<Value>>> values = new HashMap<>();
        values.put(PROPERTY_INSTANCE_OF, new HashMap<StatementRank, Set<Value>>());
        values.put(PROPERTY_SUBCLASS_OF, new HashMap<StatementRank, Set<Value>>());
        values.put(PROPERTY_GENDER, new HashMap<StatementRank, Set<Value>>());
        values.put(PROPERTY_DATE_OF_BIRTH, new HashMap<StatementRank, Set<Value>>());
        values.put(PROPERTY_COUNTRY_OF_CITIZENSHIP, new HashMap<StatementRank, Set<Value>>());
        values.put(PROPERTY_OCCUPATION, new HashMap<StatementRank, Set<Value>>());

        for (Iterator<Statement> statements = itemDocument.getAllStatements(); statements.hasNext(); ) {
            Statement statement = statements.next();
            StatementRank rank = statement.getRank();
            if (!rank.equals(StatementRank.DEPRECATED)) {
                Value value = statement.getValue();
                if (value != null) {
                    String propertyId = statement.getClaim().getMainSnak().getPropertyId().getId();
                    if (values.containsKey(propertyId)) {
                        if (!values.get(propertyId).containsKey(rank)) {
                            values.get(propertyId).put(rank, new HashSet<Value>());
                        }
                        values.get(propertyId).get(rank).add(value);
                    }
                }
            }
        }

        String itemId = itemDocument.getEntityId().getId().substring(1);

        Set<Value> instanceOf = getBestValues(values.get(PROPERTY_INSTANCE_OF));
        if (containsId(instanceOf, ITEM_HUMAN)) {
            humansCount++;
	    if (maxHumansToProcess != null){
		boolean processedMaxHumans = (humansCount > maxHumansToProcess);
		if (processedMaxHumans) {
		    throw new RuntimeException("Hardcoded stop at "+ maxHumansToProcess + " humans.");
		}

	    }

            String genderString = "\\N";
            EntityIdValue gender = (EntityIdValue) getUniqueBestValue(values.get(PROPERTY_GENDER));
            if (gender != null) {
                genderString = gender.getId().substring(1);
            } else if (!getBestValues(values.get(PROPERTY_GENDER)).isEmpty()) {
                genderString = "-1";
            }

            TimeValue dateOfBirth = (TimeValue) getUniqueBestValue(values.get(PROPERTY_DATE_OF_BIRTH));
            if (dateOfBirth != null) {
                if (dateOfBirth.getPrecision() < TimeValue.PREC_YEAR) {
                    dateOfBirth = null;
                }
            } else {
                Set<Value> bestDatesOfBirth = getBestValues(values.get(PROPERTY_DATE_OF_BIRTH));
                if (!bestDatesOfBirth.isEmpty() && isYearAlwaysEqual(bestDatesOfBirth)) {
                    dateOfBirth = (TimeValue) bestDatesOfBirth.iterator().next();
                }
            }
            String dateOfBirthString = "\\N";
            if (dateOfBirth != null) {
                dateOfBirthString = Long.toString(dateOfBirth.getYear());
            }

            Set<Long> countries = new TreeSet<>();
            addIds(countries, values.get(PROPERTY_COUNTRY_OF_CITIZENSHIP).get(StatementRank.PREFERRED));
            addIds(countries, values.get(PROPERTY_COUNTRY_OF_CITIZENSHIP).get(StatementRank.NORMAL));

            Set<Long> occupations = new TreeSet<>();
            addIds(occupations, values.get(PROPERTY_OCCUPATION).get(StatementRank.PREFERRED));
            addIds(occupations, values.get(PROPERTY_OCCUPATION).get(StatementRank.NORMAL));
            for (Long occupationId : occupations) {
                Occupation.getOccupation(occupationId).isTrueOccupation = true;
            }

            Set<String> siteLinks = new TreeSet<>();
            for (SiteLink siteLink : itemDocument.getSiteLinks().values()) {
                siteLinks.add(siteLink.getSiteKey());
            }

            try {
                humanBW.write(itemId + "," + genderString + "," + dateOfBirthString + "," + siteLinks.size() + "\n");
                for (Long country : countries) {
                    humanCountryBW.write(itemId + "," + country + "\n");
                }
                for (Long occupation : occupations) {
                    humanOccupationBW.write(itemId + "," + occupation + "\n");
                }
                for (String siteLink : siteLinks) {
                    humanSiteLinkBW.write(itemId + "," + siteLink + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

	// add a label if it's something we're interested in
	writeLabelIfNeeded(itemDocument, itemId, instanceOf);

        Set<Value> subclassOf = getBestValues(values.get(PROPERTY_SUBCLASS_OF));
        if (!subclassOf.isEmpty()) {
            Occupation occupation = Occupation.getOccupation(Long.parseLong(itemId));
            for (Value v : subclassOf) {
                EntityIdValue value = (EntityIdValue) v;
                occupation.addParent(Long.parseLong(value.getId().substring(1)));
            }
        }

    }

    private void addIds(Set<Long> ids, Set<Value> values) {
        if (values == null) {
            return;
        }
        for (Value value : values) {
            ids.add(Long.parseLong(((EntityIdValue) value).getId().substring(1)));
        }
    }

    private boolean isYearAlwaysEqual(Set<Value> values) {
        Long year = null;
        boolean isYearAlwaysEqual = true;
        for (Value value : values) {
            TimeValue timeValue = (TimeValue) value;
            if (timeValue.getPrecision() < TimeValue.PREC_YEAR) {
                isYearAlwaysEqual = false;
                break;
            }
            if (year == null) {
                year = timeValue.getYear();
            } else if (year != timeValue.getYear()) {
                isYearAlwaysEqual = false;
                break;
            }
        }
        return isYearAlwaysEqual;
    }

    private boolean containsId(Set<Value> values, String id) {
        for (Value v : values) {
            EntityIdValue value = (EntityIdValue) v;
            if (value.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void writeLabelIfNeeded(ItemDocument itemDocument, String itemId, Set<Value> instanceOf){
        // loop over the label items
        for (String labelitem: LABEL_ITEMS) {
	    //System.out.println("Label item is "+ labelitem + " itemid " + itemId);
            if (containsId(instanceOf, labelitem)) {
		//System.out.println("did contain instance of label item");
                try {
                    String label = itemDocument.findLabel("en");
                    if (label != null) {
			labelBW.write(itemId + ",\"" + label.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Value getUniqueBestValue(Map<StatementRank, Set<Value>> values) {
        Set<Value> bestValues = getBestValues(values);
        if (bestValues.size() == 1) {
            return bestValues.iterator().next();
        }
        return null;
    }

    private Set<Value> getBestValues(Map<StatementRank, Set<Value>> values) {
        if (values.containsKey(StatementRank.PREFERRED)) {
            return values.get(StatementRank.PREFERRED);
        }
        if (values.containsKey(StatementRank.NORMAL)) {
            return values.get(StatementRank.NORMAL);
        }
        return Collections.emptySet();
    }

    @Override
    public void processPropertyDocument(PropertyDocument property) {
        // nothing to do
    }

}
