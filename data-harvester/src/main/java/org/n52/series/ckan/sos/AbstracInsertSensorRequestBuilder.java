package org.n52.series.ckan.sos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.n52.sos.encode.SensorMLEncoderv101;
import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.AbstractProcess;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SmlContact;
import org.n52.sos.ogc.sensorML.SmlContactList;
import org.n52.sos.ogc.sensorML.SmlResponsibleParty;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sensorML.elements.SmlIo;
import org.n52.sos.ogc.sos.SosOffering;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.SweSimpleDataRecord;
import org.n52.sos.ogc.swe.simpleType.SweBoolean;
import org.n52.sos.ogc.swe.simpleType.SweObservableProperty;
import org.n52.sos.ogc.swe.simpleType.SweQuantity;
import org.n52.sos.ogc.swe.simpleType.SweText;
import org.n52.sos.request.InsertSensorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;
import eu.trentorise.opendata.jackan.model.CkanTag;

public abstract class AbstracInsertSensorRequestBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstracInsertSensorRequestBuilder.class);
    private boolean insitu = true;
    private boolean mobile = false;

    protected abstract AbstractProcess prepareProcedureDescription();

    public abstract String getProcedureId();

    protected abstract List<String> getObservableProperty();

    protected abstract String createProcedureLongName();

    public InsertSensorRequest create() {
        InsertSensorRequest insertSensorRequest = prepareSmlInsertSensorRequest();
        insertSensorRequest.setObservableProperty(getObservableProperty());
        insertSensorRequest.setProcedureDescriptionFormat("http://www.opengis.net/sensorML/1.0.1");
        return insertSensorRequest;
    }

    private InsertSensorRequest prepareSmlInsertSensorRequest() {
        final InsertSensorRequest insertSensorRequest = new InsertSensorRequest();
        final AbstractProcess abstractProcess = prepareProcedureDescription();

        SensorML sml = new SensorML();
        sml.addMember(abstractProcess);
        abstractProcess.setSensorDescriptionXmlString(encodeToXml(sml));

        insertSensorRequest.setAssignedOfferings(Collections.singletonList(createOffering()));
        insertSensorRequest.setAssignedProcedureIdentifier(getProcedureId());
        insertSensorRequest.setProcedureDescription(sml);
        return insertSensorRequest;
    }

    public void setInsitu(boolean insitu) {
        this.insitu = insitu;
    }

    public void setInMobile(boolean mobile) {
        this.mobile = mobile;
    }

    protected void addDatasetTags(CkanDataset dataset, List<String> keywords) {
        for (CkanTag tag : dataset.getTags()) {
            final String displayName = tag.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                keywords.add(displayName);
            }
        }
    }

    protected List<SmlIdentifier> createIdentificationList() {
        List<SmlIdentifier> idents = new ArrayList<>();
        idents.add(new SmlIdentifier(
                                     OGCConstants.UNIQUE_ID,
                                     OGCConstants.URN_UNIQUE_IDENTIFIER,
                                     // TODO check feautre id vs name
                                     getProcedureId()));
        idents.add(new SmlIdentifier(
                                     "longName",
                                     "urn:ogc:def:identifier:OGC:1.0:longName",
                                     createProcedureLongName()));
        return idents;
    }

    protected List<SmlIo<?>> createInputs(List<Phenomenon> phenomena) {
        List<SmlIo<?>> list = new ArrayList<SmlIo<?>>();
        for (Phenomenon phenomenon : phenomena) {
            list.add(createInput(phenomenon));
        }
        return list;
    }

    protected SmlIo< ? > createInput(Phenomenon phenomeon) {
        return new SmlIo<>(new SweObservableProperty().setDefinition(phenomeon.getId())).setIoName(phenomeon.getId());
    }

    protected List<SmlIo<?>> createOutputs(List<Phenomenon> phenomena) {
        List<SmlIo<?>> list = new ArrayList<SmlIo<?>>();
        for (Phenomenon phenomenon : phenomena) {
            list.add(createOutput(phenomenon));
        }
        return list;
    }

    protected SmlIo< ? > createOutput(Phenomenon phenomeon) {
        return new SmlIo<>(new SweQuantity().setUom(phenomeon.getUom()).setDefinition(phenomeon.getId())).setIoName(phenomeon.getId());
    }

    protected SmlCapabilities createMobileInsituFlagCapabilities() {
        SmlCapabilities offeringCapabilities = new SmlCapabilities("metadata");
        final SweSimpleDataRecord record = new SweSimpleDataRecord();
        record.addField(createBooleanField("insitu",
                                        "insitu",
                                        insitu));
        record.addField(createBooleanField("mobile",
                                        "mobile",
                                        mobile));
        return offeringCapabilities.setDataRecord(record);
    }

    protected SweField createTextField(String name, String definition, String value) {
        return new SweField(name, new SweText().setValue(value).setDefinition(definition));
    }

    protected SweField createBooleanField(String name, String definition, boolean value) {
        return new SweField(name, new SweBoolean().setValue(value).setDefinition(definition));
    }

    protected SmlContact createContact(CkanDataset dataset) {
        CkanOrganization organisation = dataset.getOrganization();
        SmlContactList contactList = new SmlContactList();
        final SmlResponsibleParty responsibleParty = new SmlResponsibleParty();
        responsibleParty.setOrganizationName(organisation.getTitle());
        // TODO

        contactList.addMember(responsibleParty);
        return contactList;
    }

    protected SosOffering createOffering() {
        SosOffering offering = new SosOffering(getProcedureId());
        offering.setIdentifier("Offering_" + getProcedureId());
        return offering;
    }
    protected static String encodeToXml(final SensorML sml) {
        try {
            return new SensorMLEncoderv101().encode(sml).xmlText();
        }
        catch (OwsExceptionReport ex) {
            LOGGER.error("Could not encode SML to valid XML.", ex);
            return ""; // TODO empty but valid sml
        }
    }

    protected List<String> phenomenaToIdList(List<Phenomenon> phenomena) {
        List<String> ids = new ArrayList<>();
        for (Phenomenon phenomenon : phenomena) {
            ids.add(phenomenon.getId());
        }
        return ids;
    }

    protected List<String> phenomenonToIdList(Phenomenon phenomenon) {
        List<String> ids = new ArrayList<>();
        ids.add(phenomenon.getId());
        return ids;
    }

}
