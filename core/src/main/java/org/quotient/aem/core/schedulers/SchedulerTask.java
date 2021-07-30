/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.quotient.aem.core.schedulers;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.util.*;

/**
 * A simple demo for cron-job like tasks that get executed regularly.
 * It also demonstrates how property values can be set. Users can
 * set the property values in /system/console/configMgr
 */
@Designate(ocd=SchedulerTask.Config.class)
@Component(service=Runnable.class)
public class SchedulerTask implements Runnable {

    @ObjectClassDefinition(name="A scheduled task",
                           description = "Simple demo for cron-job like task with properties")
    public static @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "*/30 * * * * ?";
    }

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Scheduler scheduler;

    private static final String JOB_NAME = "Sample Scheduler Job";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private javax.jcr.Session session;
    private ResourceResolver resourceResolver;
    private Config config;
    
    @Override
    public void run()
    {

    }

    @Activate
    protected void activate(final Config config)
    {
        this.config = config;

        try
        {
            try
            {
                this.scheduler.unschedule(JOB_NAME);
                logger.info("Removed Job:" + JOB_NAME);
            }
            catch(Exception e)
            {
                logger.info("Error removing Job:" + JOB_NAME + ":" + e.toString());
            }

            final Runnable job = new Runnable() {
                public void run() {
                    try
                    {
                        logger.info("Running Email notification...");
                        Map<String, Object> param = new HashMap<String, Object>();
                        param.put(ResourceResolverFactory.SUBSERVICE, "SchedulerTask");
                        resourceResolver = resolverFactory.getServiceResourceResolver(null);
                        session = resourceResolver.adaptTo(Session.class);

                    }
                    catch (Exception e)
                    {
                        logger.error("Run error: {}", e.toString());
                    }
                    finally
                    {
                        session.logout();
                        if(resourceResolver != null)
                        {
                            resourceResolver.close();
                        }
                    }
                }
            };

            ScheduleOptions scheduler_options = scheduler.EXPR(config.scheduler_expression());
            scheduler_options.name(JOB_NAME);

            this.scheduler.schedule(job, scheduler_options);
            logger.info("Added Job:" + JOB_NAME);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}
