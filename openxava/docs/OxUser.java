import java.util.*;

import javax.persistence.*;

@Entity
public class OxUser implements IOxUser {

	@Id
	@Column(length = 50, unique = true, nullable = false, updatable = false)
	private String username;

	@Column(length = 50, nullable = false)
	private String password;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "",
	joinColumns = @JoinColumn(name="", referencedColumnName = ""),
	inverseJoinColumns = @JoinColumn(name="", referencedColumnName = ""))
	private Collection<OxModule> modules;

	@Override
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public boolean canLogin() {
		return false;
	}

	public Collection<OxModule> getModules() {
		return modules;
	}

	public void setModules(Collection<OxModule> modules) {
		this.modules = modules;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && o instanceof OxUser
				&& this.getUsername().equals(((OxUser)o).getUsername());
	}

	@Override
	public int hashCode() {
		return this.getUsername().hashCode();
	}
}
