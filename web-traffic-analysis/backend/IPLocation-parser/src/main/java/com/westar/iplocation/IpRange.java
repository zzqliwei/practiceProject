package com.westar.iplocation;

import java.util.Objects;

public class IpRange {
    private Long startIp;
    private Long endIp;

    public IpRange(Long startIp, Long endIp) {
        this.startIp = startIp;
        this.endIp = endIp;
    }

    public Long getStartIp() {
        return startIp;
    }

    public void setStartIp(Long startIp) {
        this.startIp = startIp;
    }

    public Long getEndIp() {
        return endIp;
    }

    public void setEndIp(Long endIp) {
        this.endIp = endIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpRange ipRange = (IpRange) o;
        return Objects.equals(startIp, ipRange.startIp) &&
                Objects.equals(endIp, ipRange.endIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIp, endIp);
    }
}
