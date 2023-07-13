package helperMethods;

public class Helper {
    public String extractValue(String fullText){
        String valueFromRequest = "";
        for(int eachStringInRequest = 0; eachStringInRequest < fullText.split(" ").length; eachStringInRequest++){
            if(eachStringInRequest == 0 || eachStringInRequest == 1){
                continue;
            }
            else if (eachStringInRequest == fullText.split(" ").length - 1) {
                valueFromRequest = valueFromRequest + fullText.split(" ")[eachStringInRequest];
            }
            else{
                valueFromRequest = valueFromRequest + fullText.split(" ")[eachStringInRequest] + " ";
            }
        }
        return valueFromRequest;
    }
}
