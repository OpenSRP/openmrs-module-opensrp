package org.ei.drishti.service.reporting.rules;

import org.ei.drishti.service.reporting.ChildImmunization;
import org.ei.drishti.util.SafeMap;
import org.springframework.stereotype.Component;

import static org.ei.drishti.common.AllConstants.ChildImmunizationFields.PENTAVALENT_1_VALUE;

@Component
public class IsPentavalent1ImmunizationGivenRule implements IRule {

    @Override
    public boolean apply(SafeMap reportFields) {
        return new ChildImmunization().isImmunizedWith(PENTAVALENT_1_VALUE, reportFields);
    }

}
