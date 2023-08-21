# kecak-plugins-idempiere

## Plugin Type
- DataList Binder
- Form Load/Store Binder
- Options Binder
- Process Tool

## Overview
Access web service API in Idempiere ERP server utilizing built-in [JSON Web Service](https://wiki.idempiere.org/en/NF9_JSON_Web_Services). All web services need additional configuration as in [Web Service Security](https://wiki.idempiere.org/en/Web_Services_Security).

## Basic Configuration
All plugins require Login and Web Service Security configuration.

- Login

![image](https://github.com/kinnara-digital-studio/kecak-plugins-idempiere/assets/5206195/02e646c5-b4b3-4236-912d-2eb1d4091dca)

- Web Service Security

![image](https://github.com/kinnara-digital-studio/kecak-plugins-idempiere/assets/5206195/1779a27f-368b-4052-b287-49ebe66b6014)

**Warning !!!!** To ignore SSL (https) certificate error go to Advance configuration and check Ignore SSL Certificate Error. Be aware this means your connection from Kecak to Idempiere is not guaranteed to be secure anymore
