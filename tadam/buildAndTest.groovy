import com.getbase.plumber.tadam.FlowID
import com.getbase.plumber.tadam.FlowRunner
import com.getbase.plumber.tadam.Team
import org.jenkinsci.plugins.workflow.libs.Library

@Library('plumber@kg/mvn') _
FlowRunner.using(this).withOwnership(Team.QualityPlatform).run(FlowID.JavaMvnV1)