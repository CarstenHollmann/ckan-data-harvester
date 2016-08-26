package org.n52.series.ckan.sos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.sensorML.AbstractProcess;
import org.n52.sos.ogc.sensorML.SensorML20Constants;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.ogc.sensorML.elements.SmlClassifier;
import org.n52.sos.ogc.sensorML.elements.SmlIo;
import org.n52.sos.ogc.sos.SosOffering;
import org.n52.sos.ogc.swe.SweSimpleDataRecord;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class InsertSensorRequestBuilder extends AbstracInsertSensorRequestBuilder {

    private Phenomenon phenomenon;
    private CkanDataset dataset;
    private AbstractFeature feature;

    public InsertSensorRequestBuilder(AbstractFeature feature, Phenomenon phenomenon, CkanDataset dataset) {
        this.feature = feature;
        this.phenomenon = phenomenon;
        this.dataset = dataset;
    }

    @Override
    protected List<String> getObservableProperty() {
        return phenomenonToIdList(phenomenon);
    }

    @Override
    public String getProcedureId() {
        StringBuilder procedureId = new StringBuilder();
        procedureId.append(phenomenon.getLabel()).append("_");
        procedureId = feature.isSetName() ? procedureId.append(feature.getFirstName().getValue())
                : procedureId.append(feature.getIdentifier());
        return procedureId.toString();
    }

    @Override
    protected AbstractProcess prepareProcedureDescription() {
        final org.n52.sos.ogc.sensorML.System system = new org.n52.sos.ogc.sensorML.System();
        system.setDescription(dataset.getNotes());

        final SosOffering sosOffering = createOffering();
        system.setInputs(Collections.<SmlIo< ? >> singletonList(createInput(phenomenon)))
            .setOutputs(Collections.<SmlIo< ? >> singletonList(createOutput(phenomenon)))
            .setKeywords(createKeywordList(feature, phenomenon, dataset))
            .setIdentifications(createIdentificationList())
            .setClassifications(createClassificationList(phenomenon))
            .addCapabilities(createCapabilities(feature, sosOffering))

                // .addContact(createContact(schemaDescription.getDataset())) //
                // TODO
                // ... // TODO
                .setValidTime(createValidTimePeriod()).setIdentifier(getProcedureId());
        return system;
    }

    private List<String> createKeywordList(AbstractFeature feature,
                                           Phenomenon phenomenon,
                                           CkanDataset dataset) {
        List<String> keywords = new ArrayList<>();
        keywords.add("CKAN data");
        if (feature.isSetName()) {
            keywords.add(feature.getFirstName().getValue());
        }
        keywords.add(phenomenon.getLabel());
        keywords.add(phenomenon.getId());
        addDatasetTags(dataset, keywords);
        return keywords;
    }

    @Override
    protected String createProcedureLongName() {
        StringBuilder phenomenonName = new StringBuilder();
        phenomenonName.append(phenomenon.getLabel()).append("@");
        if (feature.isSetName()) {
            phenomenonName.append(feature.getFirstName().getValue());
        }
        else {
            phenomenonName.append(feature.getIdentifier());
        }
        return phenomenonName.toString();
    }


    private List<SmlClassifier> createClassificationList(Phenomenon phenomenon) {
        List<SmlClassifier> list = new ArrayList<SmlClassifier>();
        list.add(createClassifier(phenomenon));
        return list;
    }

    private SmlClassifier createClassifier(Phenomenon phenomenon) {
        return new SmlClassifier("phenomenon",
                               "urn:ogc:def:classifier:OGC:1.0:phenomenon",
                               null,
                               phenomenon.getId());
    }

    private TimePeriod createValidTimePeriod() {
        return new TimePeriod(new Date(), null);
    }

    private List<SmlCapabilities> createCapabilities(AbstractFeature feature,
                                                     SosOffering offering) {
        List<SmlCapabilities> capabilities = new ArrayList<>();
        capabilities.add(createFeatureCapabilities(feature));
        capabilities.add(createOfferingCapabilities(offering));
        // capabilities.add(createBboxCapabilities(feature)); // TODO
        capabilities.add(createMobileInsituFlagCapabilities());
        return capabilities;
    }

    private SmlCapabilities createFeatureCapabilities(AbstractFeature feature) {
        SmlCapabilities featuresCapabilities = new SmlCapabilities("featuresOfInterest");
        final SweSimpleDataRecord record = new SweSimpleDataRecord();
        record.addField(createTextField(
                SensorML20Constants.FEATURE_OF_INTEREST_FIELD_NAME,
                SensorML20Constants.FEATURE_OF_INTEREST_FIELD_DEFINITION,
                feature.getIdentifier()));
        return featuresCapabilities.setDataRecord(record);
    }

    private SmlCapabilities createOfferingCapabilities(SosOffering offering) {
        SmlCapabilities offeringCapabilities = new SmlCapabilities("offerings");
        final SweSimpleDataRecord record = new SweSimpleDataRecord().addField(createTextField(
                                                                                              "field_0",
                                                                                              SensorML20Constants.OFFERING_FIELD_DEFINITION,
                                                                                              offering.getIdentifier()));
        return offeringCapabilities.setDataRecord(record);
    }


    private SmlCapabilities createBboxCapabilities(AbstractFeature feature) {
        SmlCapabilities offeringCapabilities = new SmlCapabilities("observedBBOX");

        // TODO

        return offeringCapabilities;
    }

}
