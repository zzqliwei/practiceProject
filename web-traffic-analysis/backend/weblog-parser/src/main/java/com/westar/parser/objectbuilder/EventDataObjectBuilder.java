package com.westar.parser.objectbuilder;

import com.westar.parser.dataobject.BaseDataObject;
import com.westar.parser.dataobject.EventDataObject;
import com.westar.parser.utils.ColumnReader;
import com.westar.parser.utils.ParserUtils;
import com.westar.parser.utils.UrlParseUtils;
import com.westar.prepaser.PreParsedLog;

import java.util.ArrayList;
import java.util.List;

public class EventDataObjectBuilder extends AbstractDataObjectBuilder {

    @Override
    public String getCommand() {
        return "ev";
    }

    @Override
    public List<BaseDataObject> doBuildDataObjects(PreParsedLog preParsedLog) {
        List<BaseDataObject> baseDataObjects = new ArrayList<>();
        EventDataObject eventDataObject = new EventDataObject();
        ColumnReader columnReader = new ColumnReader(preParsedLog.getQueryString());
        fillCommonBaseDataObjectValue(eventDataObject, preParsedLog, columnReader);

        eventDataObject.setEventCategory(columnReader.getStringValue("eca"));
        eventDataObject.setEventAction(columnReader.getStringValue("eac"));
        eventDataObject.setEventLabel(columnReader.getStringValue("ela"));
        String eva = columnReader.getStringValue("eva");
        if (!ParserUtils.isNullOrEmptyOrDash(eva)) {
            eventDataObject.setEventValue(Float.parseFloat(eva));
        }
        eventDataObject.setUrl(columnReader.getStringValue("gsurl"));
        eventDataObject.setOriginalUrl(columnReader.getStringValue("gsourl"));
        eventDataObject.setTitle(columnReader.getStringValue("gstl"));
        eventDataObject.setHostDomain(UrlParseUtils.getInfoFromUrl(eventDataObject.getUrl()).getDomain());

        baseDataObjects.add(eventDataObject);
        return baseDataObjects;
    }
}
