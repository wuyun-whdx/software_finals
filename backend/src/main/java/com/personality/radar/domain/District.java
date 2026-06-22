package com.personality.radar.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "districts")
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    public District() {}
    public District(String name, City city) { this.name = name; this.city = city; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
}
