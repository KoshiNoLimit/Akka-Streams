public class TestMessage {
    private String url;
    private Long count;

    TestMessage(String url, Long count) {
        this.url = url;
        this.count = count;
    }

    public Long getCount() {
        return count;
    }

    public String getUrl() {
        return url;
    }
}
