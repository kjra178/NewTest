<html>
<head>
    <title>RSS to Twitter</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
</head>
<body>
<script src="/bootstrap/js/bootstrap.min.js"></script>

<div class="container">
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
        <a class="navbar-brand" href="#">RSS to Twitter</a>
        <#if accounts??>
            <#list accounts as account>
                <a href="${account.url}" target="_blank">
                    <img src="${account.logo}" width="24" height="24">
                    ${account.username}
                </a>
            </#list>
            <a href="/signout">Sign out</a>
        </#if>
    </nav>
</div>

<div class="navbar navbar-inverse navbar-fixed-top">
    <div class="navbar-inner">
        <div class="container">
            <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="brand" href="/">RSS to Twitter</a>
            <div class="nav-collapse collapse">
                <ul class="nav">
                    <#if accounts??>
                        <#list accounts as account>
                            <a href="${account.url}" target="_blank">
                                <img src="${account.logo}" width="24" height="24">
                                ${account.username}
                            </a>
                        </#list>
                        <a href="/signout">Sign out</a>
                </#if>
                </ul>
            </div><!--/.nav-collapse -->
        </div>
    </div>
</div>

<div class="container">