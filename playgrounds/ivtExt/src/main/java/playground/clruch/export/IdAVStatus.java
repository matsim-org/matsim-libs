package playground.clruch.export;

/**
 * Class for storing a <ID,String> pair
 */
class IdAVStatus {
    String idLink;
    AVStatus avStatus;

    String getStatusXmlTag() {
        return avStatus.xmlTag;
    }

}
