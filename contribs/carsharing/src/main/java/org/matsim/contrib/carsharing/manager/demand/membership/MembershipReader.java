package org.matsim.contrib.carsharing.manager.demand.membership;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
/** 
 * @author balac
 */
public class MembershipReader extends MatsimXmlParser{

	private MembershipContainer membershipContainer = new MembershipContainer();
	private Map<String, Set<String>> memberships;
	private Set<String> carsharingTypes;
	private Map<String, Set<String>> membershipPerCSType;
	private String personId;
	private String companyId;
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		if (name.equals("person")) {
			
			personId = atts.getValue("id");
			memberships = new HashMap<>();
			membershipPerCSType = new HashMap<>();
		}
		else if (name.equals("company")) {
			
			companyId = atts.getValue("id");
			carsharingTypes = new TreeSet<>();
		}
		else if (name.equals("carsharing")) {
			
			String csType = atts.getValue("name");
			if (this.membershipPerCSType.containsKey(csType)) {
				
				Set<String> companies = this.membershipPerCSType.get(csType);
				companies.add(companyId);
				this.membershipPerCSType.put(csType, companies);
				
			}
			else {
				Set<String> companies = new TreeSet<>();
				companies.add(companyId);
				this.membershipPerCSType.put(csType, companies);				
			}
			carsharingTypes.add(csType);
		}		
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		if (name.equals("person")) {
			PersonMembership personMembership = new PersonMembership(memberships, membershipPerCSType);
			membershipContainer.addPerson(personId, personMembership);
		}
		else if (name.equals("company")) {
			memberships.put(companyId, carsharingTypes);

		}		
	}
	public MembershipContainer getMembershipContainer() {
		return membershipContainer;
	}
}
