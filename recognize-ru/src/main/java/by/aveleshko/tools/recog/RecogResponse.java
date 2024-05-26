package by.aveleshko.tools.recog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"boxes"})
class RecogResponse {
    @JsonProperty("text")
    public String statusText;
    public String status;
    @JsonProperty("output_text")
    public String outputText;
}
