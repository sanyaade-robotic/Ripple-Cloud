package ds.ripple.common.XML;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public final class FeedItem {
	//private String type;
	private ItemType itemType;
	private String type;
	private ArrayList<String> values;
	private String unit;

	public enum ItemType {
		// responder's information - TBD
		// patient's information
		TEMPERATURE("temperature"),
		BLOOD_PRESSURE("bloodPressure"),
		RESPIRATION_RATE("respirationRate"),
		HEART_RATE("heartRate"),
		AGE("age"),
		O2_SATURATION("saturationO2"),
		PATIENT_FIRST_NAME("firstName"),
		PAITENT_LAST_NAME("lastName"),
		ECG("ecg"),
		CLOUDLETID("cloudLetID"),
		// cloudlet information
		CLOUDLET_REGISTRATION_MSG("cloudletRegistrationMessage"),
		CLOUDLET_DEREGISTRATION_MSG("cloudletDeregistrationMessage");
		
		private String itemType;
		
		private ItemType(final String itemType) {
			this.itemType = itemType;
		}
		
		@Override
		public String toString() {
			return this.itemType;
		}
	}
	
	protected FeedItem() {
		this.values = new ArrayList<String>();
	}
	
	protected FeedItem(ItemType type, String value) {
		this.itemType = type;
		setType(type.toString());
		this.values = new ArrayList<String>();
		this.values.add(value);
	}
	
	protected FeedItem(ItemType type, String value, String unit) {
		this.itemType = type;
		setType(type.toString());
		this.values = new ArrayList<String>();
		this.values.add(value);
		this.unit = unit;
	}
	
	protected FeedItem(ItemType type, ArrayList<String> values, String unit) {
		this.itemType = type;
		setType(type.toString());
		this.values = new ArrayList<String>(values);
		this.unit = unit;
	}
	
	public ItemType getItemType() {
		return itemType;
	}

	protected void setItemType(ItemType type) {
		this.itemType = type;
		setType(type.toString());
	}
	
	@XmlAttribute
	public String getType() {
		return type;
	}
	
	private void setType(String type) {
		this.type = type;
	}
	
	@XmlElement(name="value")
	public ArrayList<String> getValues() {
		return this.values;
	}
	
	protected void setValues(ArrayList<String> values) {
		this.values = new ArrayList<String>(values);
	}
	
	protected void setValue(String value) {
		this.values = new ArrayList<String>();
		this.values.add(value);
	}
	
	protected void addValue(String value) {
		this.values.add(value);
	}
	
	@XmlAttribute
	public String getUnit() {
		return unit;
	}

	protected void setUnit(String unit) {
		this.unit = unit;
	}
}
