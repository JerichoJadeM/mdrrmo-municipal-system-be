package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long  id;

    @Column(nullable = false, name = "first_name")
    private String firstName;

    @Column(nullable = false, name = "middle_name")
    private String middleName;

    @Column(nullable = false, name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false, name = "assignment_status")
    private String assignmentStatus; // //AVAILABLE, BUSY, OFF_DUTY

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "create_at")
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at")
    private Date updateAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    private List<Authority> authorities = new ArrayList<>();

    @Lob
    @Column(name = "profile_image_url", columnDefinition = "LONGTEXT")
    private String profileImageUrl;

    @Column(name = "position")
    private String position;

    @Column(name = "office")
    private String office;

    @Column(name = "account_status", nullable = false)
    private String accountStatus = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED

    @Column(name = "responder_eligible", columnDefinition = "TINYINT(1) default 0")
    private Boolean responderEligible = false;

    @Column(name = "coordinator_eligible", columnDefinition = "TINYINT(1) default 0")
    private Boolean coordinatorEligible = false;

    public User(){}

    public User(String s, String string, String lastName, String email, String password, String position, String office, String accountStatus, Boolean aBoolean, Boolean coordinatorEligible, List<String> authorities) {}

    public User(String firstName, String middleName, String lastName, String email, String number, String password) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.number = number;
        this.password = password;

    }

    public User(String firstName, String middleName, String lastName, String email, String number, String assignmentStatus, String password, Date createAt, Date updateAt, List<Authority> authorities, String profileImageUrl, String position, String office, String accountStatus, Boolean responderEligible, Boolean coordinatorEligible) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.number = number;
        this.assignmentStatus = assignmentStatus;
        this.password = password;
        this.createAt = createAt;
        this.updateAt = updateAt;
        this.authorities = authorities;
        this.profileImageUrl = profileImageUrl;
        this.position = position;
        this.office = office;
        this.accountStatus = accountStatus;
        this.responderEligible = responderEligible;
        this.coordinatorEligible = coordinatorEligible;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assigmentStatus) {
        this.assignmentStatus = assigmentStatus;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public Date getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<Authority> authorities) {
        this.authorities = new ArrayList<>(authorities);
    }

    public void updateAuthorities(List<Authority> newAuthorities) {
        if (this.authorities == null) {
            this.authorities = new ArrayList<>();
        }
        this.authorities.clear();
        this.authorities.addAll(newAuthorities);
    }

    // convenience method for full name
    @Transient
    public String getFullName() {
        return String.join(" ",
                firstName != null ? firstName.trim() : "",
                middleName != null && !middleName.isBlank() ? middleName.trim() : "",
                lastName != null ? lastName.trim() : ""
        ).trim().replaceAll("\\s+", " ");
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Boolean getResponderEligible() {
        return responderEligible;
    }

    public void setResponderEligible(Boolean responderEligible) {
        this.responderEligible = responderEligible;
    }

    public Boolean getCoordinatorEligible() {
        return coordinatorEligible;
    }

    public void setCoordinatorEligible(Boolean coordinatorEligible) {
        this.coordinatorEligible = coordinatorEligible;
    }
}
