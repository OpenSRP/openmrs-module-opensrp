package org.ei.drishti.view.dialog;

import static org.ei.drishti.view.contract.SmartRegisterClient.ST_COMPARATOR;

import java.util.Collections;

import org.ei.drishti.Context;
import org.ei.drishti.R;
import org.ei.drishti.view.contract.SmartRegisterClients;

public class STSort implements SortOption {
    @Override
    public String name() {
        return Context.getInstance().getStringResource(R.string.sort_by_st_label);
    }

    @Override
    public SmartRegisterClients sort(SmartRegisterClients allClients) {
        Collections.sort(allClients, ST_COMPARATOR);
        return allClients;
    }
}
