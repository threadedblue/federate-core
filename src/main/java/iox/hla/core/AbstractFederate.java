package iox.hla.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.ieee.standards.ieee1516._2010.InteractionClassType;
import org.ieee.standards.ieee1516._2010.ObjectModelType;
import org.ieee.standards.ieee1516._2010.SharingEnumerations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.TimeConstrainedAlreadyEnabled;
import hla.rti1516e.exceptions.TimeRegulationAlreadyEnabled;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64TimeFactory;

public abstract class AbstractFederate implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(AbstractFederate.class);

	public static final String INTERACTION_NAME_ROOT = "HLAinteractionRoot";
	public static final String OBJECT_NAME_ROOT = "HLAobjectRoot";
	public static final String DEFAULT_CONFIG_FILE = "conf/config.yml";
	public static final String THREAD_NAME = "FederateAmbassador";

	public static final int MAX_JOIN_ATTEMPTS = 6;
	public static final int REJOIN_DELAY_MS = 10000;

	protected RTIambassador rtiAmb;

	public enum FEDERATION_EVENTS {
		CREATING_FEDERATION, FEDERATION_CREATED
	}

	public enum State {
		CONSTRUCTED, INITIALIZED, JOINED, TERMINATING;
	};

	public static enum SYNCH_POINTS {
		readyToPopulate, readyToRun, readyToResign
	};

	protected HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory; // set when we join

	protected String federateName;
	protected String federationName;
	protected double stepSize;
	protected double lookahead;
	protected double logicalTime = 0D;
	private AtomicBoolean advancing = new AtomicBoolean(false);
	protected FederateAmbassador fedAmb;
	protected ObjectModelType fom;

	public AbstractFederate() {
		super();
		try {
			this.rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
			this.encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
			this.fedAmb = new FederateAmbassador();
		} catch (RTIinternalError e) {
			System.err.println(e);
		}
	}
	protected void enableTimeConstrained() throws RTIAmbassadorException {
		try {
			log.info("enabling time constrained");
			rtiAmb.enableTimeConstrained();
			while (!fedAmb.isTimeConstrained()) {
				tick();
			}
		} catch (TimeConstrainedAlreadyEnabled e) {
			log.info("time constrained already enabled");
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	protected void enableTimeRegulation() throws RTIAmbassadorException {
		try {
			log.info("enabling time regulation");
			HLAfloat64Interval lookahead = getTimeFactory().makeInterval(getLookahead());
			rtiAmb.enableTimeRegulation(lookahead);
			while (!fedAmb.isTimeRegulating()) {
				tick();
			}
		} catch (TimeRegulationAlreadyEnabled e) {
			log.info("time regulation already enabled");
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}
	
	public void enableAsynchronousDelivery() throws RTIAmbassadorException {
		try {
			log.info("enabling asynchronous delivery of receive order messages");
			rtiAmb.enableAsynchronousDelivery();
		} catch (RTIexception e) {
			throw new RTIAmbassadorException(e);
		}
	}

	public EClass findEClass(String objectName) {
		EClassifier eClassifier = null;
		// objectName comes through HLA with a prefix prepended.  We gotta strip it off.
		String[] ss = objectName.split("\\.");
		for (Map.Entry<String, Object> entry : EPackage.Registry.INSTANCE.entrySet()) {
			String key = entry.getKey();
			EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(key);
			eClassifier = ePackage.getEClassifier(ss[1]);
			if (eClassifier != null) {
				break;
			}
		}
		return (EClass) eClassifier;
	}

	public abstract void readyToPopulate();

	public abstract void readyToRun();

	public void tick() {
		try {
			rtiAmb.evokeMultipleCallbacks(0.1, 0.2);
		} catch (CallNotAllowedFromWithinCallback | RTIinternalError e) {
			log.error("", e);
		}
	}
	
	public RTIambassador getRtiAmb() {
		return rtiAmb;
	}

	public void setRtiAmb(RTIambassador rtiAmb) {
		this.rtiAmb = rtiAmb;
	}

	public HLAfloat64TimeFactory getTimeFactory() {
		return timeFactory;
	}

	public void setTimeFactory(HLAfloat64TimeFactory timeFactory) {
		this.timeFactory = timeFactory;
	}

	public EncoderFactory getEncoderFactory() {
		return encoderFactory;
	}

	public void setEncoderFactory(EncoderFactory encoderFactory) {
		this.encoderFactory = encoderFactory;
	}

	public String getFederateName() {
		return federateName;
	}

	public void setFederateName(String federateName) {
		this.federateName = federateName;
	}

	public String getFederationName() {
		return federationName;
	}

	public void setFederationName(String federationName) {
		this.federationName = federationName;
	}

	public double getStepSize() {
		return stepSize;
	}

	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

	public double getLookahead() {
		return lookahead;
	}

	public void setLookahead(double lookahead) {
		this.lookahead = lookahead;
	}

	public double getLogicalTime() {
		return logicalTime;
	}

	public void setLogicalTime(Double logicalTime) {
		this.logicalTime = logicalTime;
	}

	public Set<InteractionClassType> getInteractionSubscribe() {
		Set<InteractionClassType> set = new HashSet<InteractionClassType>();
		for (InteractionClassType itr : fom.getInteractions().getInteractionClass().getInteractionClass()) {
			getInteractions(set, itr, SharingEnumerations.SUBSCRIBE);
		}

		return set;
	}

	public Set<InteractionClassType> getInteractionPublish() {
		Set<InteractionClassType> set = new HashSet<InteractionClassType>();
		for (InteractionClassType itr : fom.getInteractions().getInteractionClass().getInteractionClass()) {
			getInteractions(set, itr, SharingEnumerations.PUBLISH);
		}

		return set;
	}

	public Set<InteractionClassType> getInteractions(Set<InteractionClassType> set, InteractionClassType itr,
			SharingEnumerations pubsub) {
		if (itr.getSharing() != null) {
			if (itr.getSharing().getValue() == pubsub
					|| itr.getSharing().getValue() == SharingEnumerations.PUBLISH_SUBSCRIBE) {
				set.add(itr);
				log.trace("added InteractionClassType.name=" + itr.getName().getValue() + "size=" + set.size());
			}
		}
		for (InteractionClassType itr1 : itr.getInteractionClass()) {
			getInteractions(set, itr1, pubsub);
		}
		return set;
	}

}
