import * as React from 'react'
import {
  Controller,
  FormProvider,
  useFormContext,
  type ControllerProps,
  type FieldPath,
  type FieldValues,
} from 'react-hook-form'
import { cn } from 'cn'

const Form = FormProvider

interface FormFieldContextValue {
  name: string
}

const FormFieldContext = React.createContext<FormFieldContextValue | null>(null)

interface FormItemContextValue {
  id: string
}

const FormItemContext = React.createContext<FormItemContextValue | null>(null)

function FormField<TFieldValues extends FieldValues, TName extends FieldPath<TFieldValues>>(
  props: ControllerProps<TFieldValues, TName>,
) {
  return (
    <FormFieldContext.Provider value={{ name: props.name }}>
      <Controller {...props} />
    </FormFieldContext.Provider>
  )
}

function useFormField() {
  const fieldContext = React.useContext(FormFieldContext)
  const itemContext = React.useContext(FormItemContext)
  const { getFieldState, formState } = useFormContext()

  if (!fieldContext || !itemContext) {
    throw new Error('Os componentes de formulário devem estar dentro de FormField e FormItem.')
  }

  const fieldState = getFieldState(fieldContext.name, formState)
  const descriptionId = `${itemContext.id}-description`
  const messageId = `${itemContext.id}-message`

  return {
    ...fieldState,
    name: fieldContext.name,
    formItemId: itemContext.id,
    formDescriptionId: descriptionId,
    formMessageId: messageId,
  }
}

function FormItem({ className, ...props }: React.ComponentProps<'div'>) {
  const id = React.useId()

  return (
    <FormItemContext.Provider value={{ id }}>
      <div className={cn('space-y-2', className)} {...props} />
    </FormItemContext.Provider>
  )
}

function FormLabel({ className, ...props }: React.ComponentProps<'label'>) {
  const { error, formItemId } = useFormField()

  return (
    <label
      htmlFor={formItemId}
      className={cn(
        'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
        error && 'text-destructive',
        className,
      )}
      {...props}
    />
  )
}

function FormControl({
  children,
}: {
  children: React.ReactElement<{
    id?: string
    'aria-describedby'?: string
    'aria-invalid'?: boolean
  }>
}) {
  const { error, formDescriptionId, formItemId, formMessageId } = useFormField()
  const describedBy = error
    ? `${formDescriptionId} ${formMessageId}`
    : formDescriptionId

  return React.cloneElement(children, {
    id: formItemId,
    'aria-describedby': describedBy,
    'aria-invalid': Boolean(error),
  })
}

function FormDescription({ className, ...props }: React.ComponentProps<'p'>) {
  const { formDescriptionId } = useFormField()

  return (
    <p
      id={formDescriptionId}
      className={cn('text-sm text-muted-foreground', className)}
      {...props}
    />
  )
}

function FormMessage({ className, children, ...props }: React.ComponentProps<'p'>) {
  const { error, formMessageId } = useFormField()
  const message = error?.message
  const body = children ?? (typeof message === 'string' ? message : undefined)

  if (!body) {
    return null
  }

  return (
    <p
      id={formMessageId}
      role="alert"
      className={cn('text-sm font-medium text-destructive', className)}
      {...props}
    >
      {body}
    </p>
  )
}

export { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage }
