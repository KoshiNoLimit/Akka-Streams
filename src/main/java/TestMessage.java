public class TestMessage {
    private String url;
    private Integer count;

    TestMessage(String url, Integer count) {
        this.url = url;
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public String getUrl() {
        return url;
    }
}
