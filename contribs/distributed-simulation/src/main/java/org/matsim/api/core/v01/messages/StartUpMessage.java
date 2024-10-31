package org.matsim.api.core.v01.messages;

import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Message;

@Builder
@Data
public class StartUpMessage implements Message {

    private final String[] linkIds;

    private final String[] nodeIds;

    private final String[] personIds;

}
