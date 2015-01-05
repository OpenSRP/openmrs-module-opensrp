package org.ei.drishti.view.contract;

import java.util.ArrayList;

import org.ei.drishti.util.StringUtil;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class Villages extends ArrayList<Village> {
    public Iterable<String> getVillageNames() {
        return Iterables.transform(this, new Function<Village, String>() {
            @Override
            public String apply(Village village) {
                return StringUtil.humanize(village.name());
            }
        });
    }
}
