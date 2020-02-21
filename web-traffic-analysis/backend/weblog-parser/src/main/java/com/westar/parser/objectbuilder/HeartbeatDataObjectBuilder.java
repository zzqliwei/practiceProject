package com.westar.parser.objectbuilder;

import com.westar.parser.dataobject.BaseDataObject;
import com.westar.parser.dataobject.HeartbeatDataObject;
import com.westar.parser.utils.ColumnReader;
import com.westar.parser.utils.ParserUtils;
import com.westar.prepaser.PreParsedLog;

import java.util.ArrayList;
import java.util.List;

public class HeartbeatDataObjectBuilder extends AbstractDataObjectBuilder {

    @Override
    public String getCommand() {
        return "hb";
    }

    @Override
    public List<BaseDataObject> doBuildDataObjects(PreParsedLog preParsedLog) {
        List<BaseDataObject> dataObjects = new ArrayList<>();
        HeartbeatDataObject dataObject = new HeartbeatDataObject();
        ColumnReader columnReader = new ColumnReader(preParsedLog.getQueryString());
        fillCommonBaseDataObjectValue(dataObject, preParsedLog, columnReader);

        int loadingDuration = 0;
        String plt = columnReader.getStringValue("plt");
        if (!ParserUtils.isNullOrEmptyOrDash(plt)) {
            loadingDuration = Math.round(Float.parseFloat(plt));
        }
        dataObject.setLoadingDuration(loadingDuration);

        int clientPageDuration = 0;
        String psd = columnReader.getStringValue("psd");
        if (!ParserUtils.isNullOrEmptyOrDash(psd)) {
            clientPageDuration = Math.round(Float.parseFloat(psd));
        }
        dataObject.setClientPageDuration(clientPageDuration);

        dataObjects.add(dataObject);
        return dataObjects;
    }
}
