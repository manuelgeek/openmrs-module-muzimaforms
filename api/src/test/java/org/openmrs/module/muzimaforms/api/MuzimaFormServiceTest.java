package org.openmrs.module.muzimaforms.api;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.muzimaforms.MuzimaForm;
import org.openmrs.module.muzimaforms.api.db.hibernate.MuzimaFormDAO;
import org.openmrs.module.muzimaforms.api.impl.CompositeEnketoResult;
import org.openmrs.module.muzimaforms.api.impl.EnketoResult;
import org.openmrs.module.muzimaforms.api.impl.MuzimaFormServiceImpl;
import org.openmrs.module.muzimaforms.xForm2MuzimaTransform.ModelXml2JsonTransformer;
import org.openmrs.module.muzimaforms.xForm2MuzimaTransform.XForm2Html5Transformer;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.openmrs.module.muzimaforms.MuzimaFormBuilder.muzimaform;
import static org.openmrs.module.muzimaforms.MuzimaFormTagBuilder.tag;
import static org.openmrs.module.muzimaforms.XFormBuilder.xForm;

public class MuzimaFormServiceTest {

    private MuzimaFormService service;
    MuzimaFormDAO dao;
    XForm2Html5Transformer transformer;
    ModelXml2JsonTransformer modelTransformer;

    @Before
    public void setUp() throws Exception {
        dao = mock(MuzimaFormDAO.class);
        transformer = mock(XForm2Html5Transformer.class);
        modelTransformer = mock(ModelXml2JsonTransformer.class);
        service = new MuzimaFormServiceImpl(dao, transformer, modelTransformer);
    }

    void setUpDao() {
        List<MuzimaForm> muzimaForms = new ArrayList<MuzimaForm>();
        muzimaForms.add(
                muzimaform().withId(1).withName("Registration Form").withDescription("Form for registration")
                        .with(tag().withId(1).withName("Registration"))
                        .with(tag().withId(2).withName("Patient"))
                        .instance());
        muzimaForms.add(muzimaform().withId(2).withName("PMTCT Form").withDescription("Form for PMTCT")
                .with(tag().withId(1).withName("Registration"))
                .with(tag().withId(3).withName("Encounter"))
                .with(tag().withId(4).withName("HIV"))
                .instance());

        muzimaForms.add(muzimaform().withId(3).withName("Ante-Natal Form").withDescription("Form for ante-natal care")
                .instance());

        when(dao.getAll()).thenReturn(muzimaForms);
    }

    @Test
    public void getAll_shouldGetAllForms() throws Exception {
        setUpDao();
        List<MuzimaForm> list = service.getAll();
        assertThat(list.size(), is(3));
        verify(dao, times(1)).getAll();
    }

    @Test
    public void getXform_shouldLoadXForm() throws Exception {
        service.getXForms();
        verify(dao, times(1)).getXForms();
    }

    @Test
    public void importExisting_shouldRetrieveExistingXFormAndConvertItIntoHTML5AndPersistAMuzimaForm() throws Exception {
        String xFormXml = "<xml><some/><valid/></xml>";
        String htmlForm = "<foo><form><ul><li/><li/></ul></form><model/></foo>";
        String modelJson = "{form : [{name:'', bind: ''}]}";

        when(dao.getXform(1)).thenReturn(xForm().withId(1).withXFormXml(xFormXml).instance());
        when(transformer.transform(xFormXml)).thenReturn(new EnketoResult(htmlForm));
        when(modelTransformer.transform(htmlForm)).thenReturn(new CompositeEnketoResult(htmlForm, modelJson));

        service.importExisting(1, "name", "description");

        verify(dao, times(1)).getXform(1);
        verify(dao, times(1)).saveForm(muzimaform()
                .withName("name")
                .withDescription("description")
                .instance());
    }

    @Test
    public void importExisting_shouldSetConvertedXform() throws Exception {

        String htmlForm = "<foo><form><ul><li/><li/></ul></form><model/></foo>";
        String xFormXml = "<foo><some/><valid/></foo>";
        String modelJson = "{form : [{name:'', bind: ''}]}";

        when(transformer.transform(xFormXml)).thenReturn(new EnketoResult(htmlForm));
        when(modelTransformer.transform(htmlForm)).thenReturn(new CompositeEnketoResult(htmlForm, modelJson));
        when(dao.getXform(1)).thenReturn(xForm().withId(1).withXFormXml(xFormXml).instance());

        service.importExisting(1, "name", "description");

        verify(dao, times(1)).saveForm(muzimaform().withName("name").withDescription("description")
                .instance());
        verify(transformer, times(1)).transform(xFormXml);
        verify(modelTransformer, times(1)).transform(htmlForm);
        verify(dao, times(1)).getXform(1);
    }

    @Test
    public void save_shouldSaveExistingForm() throws Exception {
        MuzimaForm form = muzimaform().withId(1).instance();
        service.save(form);
        verify(dao).saveForm(form);
    }

    @Test
    public void findById_shouldFindFormById() {
        service.findById(1);
        verify(dao, times(1)).findById(1);
    }

    @Test
    public void findByUUID_shouldFindFormByUUID() {
        service.findByUniqueId("foo");
        verify(dao, times(1)).findByUuid("foo");
    }
}
