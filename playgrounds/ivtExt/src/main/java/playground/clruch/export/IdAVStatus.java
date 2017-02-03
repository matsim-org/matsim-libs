package playground.clruch.export;

/**
 * Class for storing a <ID,String> pair
 */
@Deprecated
class IdAVStatus {
    String idLink;
    AVStatus avStatus;

    String getStatusXmlTag() {
        return avStatus.xmlTag;
    }

}
