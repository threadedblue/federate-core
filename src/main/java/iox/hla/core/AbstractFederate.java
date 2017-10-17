package iox.hla.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.time.HLAfloat64TimeFactory;

public abstract class AbstractFederate {

	private static final Logger log = LoggerFactory.getLogger(AbstractFederate.class);

	public static final String INTERACTION_NAME_ROOT = "HLAinteractionRoot";
	public static final String OBJECT_NAME_ROOT = "HLAobjectRoot";
	public static final String DEFAULT_CONFIG_FILE = "conf/config.yml";

	public static final int MAX_JOIN_ATTEMPTS = 6;
	public static final int REJOIN_DELAY_MS = 10000;

	protected RTIambassador rtiAmb;

	public enum FEDERATION_EVENTS {
		CREATING_FEDERATION, FEDERATION_CREATED
	}

	public enum State {
		CONSTRUCTED, INITIALIZED, JOINED, TERMINATING;
	};

	public enum SYNCH_POINTS {
		readyToPopulate, readyToRun, readyToResign
	};

	protected HLAfloat64TimeFactory timeFactory; // set when we join
	protected EncoderFactory encoderFactory; // set when we join

	protected String federateName;
	protected String federationName;
	protected double stepsize;
	protected double lookahead;
	protected Double logicalTime;

	public AbstractFederate() {
		super();
		try {
			this.rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		} catch (RTIinternalError e) {
			System.err.println(e);
		}
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

	public double getStepsize() {
		return stepsize;
	}

	public void setStepsize(double stepsize) {
		this.stepsize = stepsize;
	}

	public double getLookahead() {
		return lookahead;
	}

	public void setLookahead(double lookahead) {
		this.lookahead = lookahead;
	}

	public Double getLogicalTime() {
		return logicalTime;
	}

	public void setLogicalTime(Double logicalTime) {
		this.logicalTime = logicalTime;
	}

}
