<h2>RSS to Twitter</h2>

<#if account??>
	${account.username}
<#else>
	<a href="oauth/login">Sign in</a>
</#if>

<#if account??>
	<h4>Feeds</h4>
	<p>These feeds will be tweeted to your Twitter account ${account.username}.</p>
	<p><a href="/feeds/new">Add new</a></p>
	<ul>
	    <#list jobs as job>
        	<li>
        	    <p><a href="/feeds/${job.objectId}">${job.feed.url}</a><p>
        	</li>
        </#list>
	</ul>
</#if>


